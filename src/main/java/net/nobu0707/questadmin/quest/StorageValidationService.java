package net.nobu0707.questadmin.quest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public final class StorageValidationService {
    private static final Pattern INTEGER_PATTERN = Pattern.compile("-?\\d+");

    public ValidationReport validate(Path questsPath, Path playerQuestsPath) {
        QuestFileValidationResult quests = validateQuests(questsPath);
        PlayerQuestFileValidationResult playerQuests = validatePlayerQuests(playerQuestsPath, quests.validQuestIds(), quests.readable());
        return new ValidationReport(quests, playerQuests);
    }

    private QuestFileValidationResult validateQuests(Path questsPath) {
        List<StorageValidationIssue> issues = new ArrayList<>();
        Set<String> validQuestIds = new HashSet<>();
        int validCount = 0;
        int skippedCount = 0;

        JsonElement root;
        try {
            root = readJson(questsPath);
        } catch (IOException | JsonParseException | IllegalArgumentException exception) {
            issues.add(StorageValidationIssue.error("quests.json", "JSONを読み込めません: " + exception.getMessage()));
            return new QuestFileValidationResult(false, 0, 0, Set.of(), List.copyOf(issues), false);
        }

        if (!root.isJsonArray()) {
            issues.add(StorageValidationIssue.error("quests.json", "ルートはJSON配列で指定してください。"));
            return new QuestFileValidationResult(false, 0, 0, Set.of(), List.copyOf(issues), true);
        }

        for (int index = 0; index < root.getAsJsonArray().size(); index++) {
            JsonElement element = root.getAsJsonArray().get(index);
            QuestDefinition quest;
            try {
                quest = decodeQuest(element);
            } catch (IllegalArgumentException exception) {
                skippedCount++;
                issues.add(StorageValidationIssue.error("quests.json", (index + 1) + "番目のクエスト: " + exception.getMessage()));
                continue;
            }

            QuestValidationResult validationResult = QuestValidator.validateForLoad(quest, validQuestIds);
            if (!validationResult.isValid()) {
                skippedCount++;
                issues.add(StorageValidationIssue.error("quests.json", (index + 1) + "番目のクエスト: " + validationResult.joinedMessages()));
                continue;
            }

            validCount++;
            validQuestIds.add(quest.getId());
        }

        boolean success = issues.stream().noneMatch(StorageValidationIssue::error);
        return new QuestFileValidationResult(success, validCount, skippedCount, Set.copyOf(validQuestIds), List.copyOf(issues), true);
    }

    private PlayerQuestFileValidationResult validatePlayerQuests(Path playerQuestsPath, Set<String> validQuestIds, boolean canCheckQuestIds) {
        List<StorageValidationIssue> issues = new ArrayList<>();
        int playerCount = 0;
        int entryCount = 0;

        JsonElement root;
        try {
            if (Files.notExists(playerQuestsPath)) {
                return new PlayerQuestFileValidationResult(true, 0, 0, List.of());
            }
            root = readJson(playerQuestsPath);
        } catch (IOException | JsonParseException | IllegalArgumentException exception) {
            issues.add(StorageValidationIssue.error("player_quests.json", "JSONを読み込めません: " + exception.getMessage()));
            return new PlayerQuestFileValidationResult(false, 0, 0, List.copyOf(issues));
        }

        if (!root.isJsonObject()) {
            issues.add(StorageValidationIssue.error("player_quests.json", "ルートはJSONオブジェクトで指定してください。"));
            return new PlayerQuestFileValidationResult(false, 0, 0, List.copyOf(issues));
        }

        if (!canCheckQuestIds) {
            issues.add(StorageValidationIssue.warn("player_quests.json", "quests.jsonを検証できないためquestId照合をスキップしました。"));
        }

        for (String playerUuidText : root.getAsJsonObject().keySet()) {
            UUID playerUuid;
            try {
                playerUuid = UUID.fromString(playerUuidText);
            } catch (IllegalArgumentException exception) {
                issues.add(StorageValidationIssue.error("player_quests.json", "UUID形式が不正です: " + playerUuidText));
                continue;
            }

            JsonElement playerValue = root.getAsJsonObject().get(playerUuidText);
            if (!playerValue.isJsonObject()) {
                issues.add(StorageValidationIssue.error("player_quests.json", playerUuid + " の値はJSONオブジェクトで指定してください。"));
                continue;
            }

            playerCount++;
            JsonObject questStates = playerValue.getAsJsonObject();
            for (String questId : questStates.keySet()) {
                try {
                    JsonElement questState = questStates.get(questId);
                    if (!questState.isJsonObject()) {
                        issues.add(StorageValidationIssue.error("player_quests.json", playerUuid + " / " + questId + " の状態はJSONオブジェクトで指定してください。"));
                        continue;
                    }

                    JsonObject stateObject = questState.getAsJsonObject();
                    String statusName = readString(stateObject, "status", "status");
                    if (parseStatus(statusName) == null) {
                        issues.add(StorageValidationIssue.warn("player_quests.json", playerUuid + " / " + questId + " に不明なstatusがあります: " + statusName));
                        continue;
                    }

                    readLong(stateObject, "completedAt", "completedAt");
                    readLong(stateObject, "claimedAt", "claimedAt");
                    entryCount++;

                    if (canCheckQuestIds && !validQuestIds.contains(questId)) {
                        issues.add(StorageValidationIssue.warn("player_quests.json", "削除済みまたは不明なquestIdがあります: " + questId));
                    }
                } catch (IllegalArgumentException exception) {
                    issues.add(StorageValidationIssue.error("player_quests.json", playerUuid + " / " + questId + ": " + exception.getMessage()));
                    continue;
                }
            }
        }

        boolean success = issues.stream().noneMatch(StorageValidationIssue::error);
        return new PlayerQuestFileValidationResult(success, playerCount, entryCount, List.copyOf(issues));
    }

    private static JsonElement readJson(Path path) throws IOException {
        if (Files.notExists(path)) {
            throw new IllegalArgumentException("ファイルが存在しません: " + path);
        }
        return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8));
    }

    private static QuestDefinition decodeQuest(JsonElement element) {
        if (!element.isJsonObject()) {
            throw new IllegalArgumentException("クエスト定義はJSONオブジェクトで指定してください。");
        }

        JsonObject object = element.getAsJsonObject();
        JsonObject requirement = readObject(object, "requirement", "requirement");
        JsonObject reward = readObject(object, "reward", "reward");
        String typeName = readString(object, "type", "type");
        if (!QuestType.ITEM_DELIVERY.name().equals(typeName)) {
            throw new IllegalArgumentException("QuestAdmin: type は ITEM_DELIVERY のみ対応です: " + typeName);
        }

        return new QuestDefinition(
                readString(object, "id", "id"),
                readString(object, "title", "title"),
                readOptionalString(object, "description"),
                QuestType.ITEM_DELIVERY,
                new QuestRequirement(
                        readString(requirement, "itemId", "requirement.itemId"),
                        readInt(requirement, "amount", "requirement.amount")
                ),
                new QuestReward(readLong(reward, "money", "reward.money")),
                readBoolean(object, "repeatable", "repeatable"),
                readBoolean(object, "enabled", "enabled"),
                readLong(object, "createdAt", "createdAt"),
                readLong(object, "updatedAt", "updatedAt")
        );
    }

    private static JsonObject readObject(JsonObject object, String key, String fieldName) {
        JsonElement value = object.get(key);
        if (value != null && value.isJsonObject()) {
            return value.getAsJsonObject();
        }
        throw new IllegalArgumentException(fieldName + " はJSONオブジェクトで指定してください。");
    }

    private static String readString(JsonObject object, String key, String fieldName) {
        JsonElement value = object.get(key);
        if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            return value.getAsString();
        }
        throw new IllegalArgumentException(fieldName + " は文字列で指定してください。");
    }

    private static String readOptionalString(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull()) {
            return "";
        }
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            return value.getAsString();
        }
        throw new IllegalArgumentException("description は文字列で指定してください。");
    }

    private static int readInt(JsonObject object, String key, String fieldName) {
        long value = readLong(object, key, fieldName);
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(fieldName + " はint範囲内の整数で指定してください。");
        }
        return (int) value;
    }

    private static long readLong(JsonObject object, String key, String fieldName) {
        JsonElement value = object.get(key);
        if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
            if (!INTEGER_PATTERN.matcher(value.getAsString()).matches()) {
                throw new IllegalArgumentException(fieldName + " は整数で指定してください。");
            }
            try {
                return value.getAsLong();
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(fieldName + " は整数で指定してください。");
            }
        }
        throw new IllegalArgumentException(fieldName + " は整数で指定してください。");
    }

    private static boolean readBoolean(JsonObject object, String key, String fieldName) {
        JsonElement value = object.get(key);
        if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()) {
            return value.getAsBoolean();
        }
        throw new IllegalArgumentException(fieldName + " は true または false で指定してください。");
    }

    private static QuestStatus parseStatus(String statusName) {
        try {
            return QuestStatus.valueOf(statusName);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public record ValidationReport(QuestFileValidationResult quests, PlayerQuestFileValidationResult playerQuests) {
        public boolean success() {
            return quests.success() && playerQuests.success();
        }
    }

    public record QuestFileValidationResult(
            boolean success,
            int validCount,
            int skippedCount,
            Set<String> validQuestIds,
            List<StorageValidationIssue> issues,
            boolean readable
    ) {
    }

    public record PlayerQuestFileValidationResult(
            boolean success,
            int playerCount,
            int entryCount,
            List<StorageValidationIssue> issues
    ) {
        public long warningCount() {
            return issues.stream().filter(StorageValidationIssue::warning).count();
        }
    }

    public record StorageValidationIssue(Severity severity, String fileName, String message) {
        private static StorageValidationIssue error(String fileName, String message) {
            return new StorageValidationIssue(Severity.ERROR, fileName, message);
        }

        private static StorageValidationIssue warn(String fileName, String message) {
            return new StorageValidationIssue(Severity.WARN, fileName, message);
        }

        public boolean error() {
            return severity == Severity.ERROR;
        }

        public boolean warning() {
            return severity == Severity.WARN;
        }
    }

    public enum Severity {
        ERROR,
        WARN
    }
}
