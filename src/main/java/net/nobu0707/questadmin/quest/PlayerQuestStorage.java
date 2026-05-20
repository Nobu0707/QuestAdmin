package net.nobu0707.questadmin.quest;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class PlayerQuestStorage {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final Path playerQuestsPath;
    private Map<UUID, Map<String, PlayerQuestState>> states = new LinkedHashMap<>();

    public PlayerQuestStorage(Path playerQuestsPath) {
        this.playerQuestsPath = playerQuestsPath;
    }

    public static PlayerQuestStorage createDefault(Path configDirectory) {
        return new PlayerQuestStorage(configDirectory.resolve("questadmin").resolve("player_quests.json"));
    }

    public LoadResult load() {
        if (Files.notExists(playerQuestsPath)) {
            states = new LinkedHashMap<>();
            return LoadResult.success(states);
        }

        try {
            String json = Files.readString(playerQuestsPath, StandardCharsets.UTF_8);
            states = decodePlayerQuestStates(json);
            return LoadResult.success(states);
        } catch (IOException | RuntimeException exception) {
            LOGGER.error("Failed to load player quest states from {}.", playerQuestsPath, exception);
            return LoadResult.failure(states, exception.getMessage());
        }
    }

    public void save() throws IOException {
        StorageFileUtil.writeStringSafely(playerQuestsPath, encodePlayerQuestStates(states), LOGGER, StorageKind.PLAYER_QUESTS);
    }

    public Path getPlayerQuestsPath() {
        return playerQuestsPath;
    }

    public Optional<PlayerQuestState> getState(UUID playerUuid, String questId) {
        Map<String, PlayerQuestState> playerStates = states.get(playerUuid);
        if (playerStates == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(playerStates.get(questId));
    }

    public Map<String, PlayerQuestState> getStates(UUID playerUuid) {
        Map<String, PlayerQuestState> playerStates = states.get(playerUuid);
        if (playerStates == null) {
            return Map.of();
        }
        return Collections.unmodifiableMap(playerStates);
    }

    public void setState(UUID playerUuid, String questId, PlayerQuestState state) {
        states.computeIfAbsent(playerUuid, ignored -> new LinkedHashMap<>()).put(questId, state);
    }

    public boolean hasCompleted(UUID playerUuid, String questId) {
        return getState(playerUuid, questId)
                .map(state -> state.getStatus() == QuestStatus.COMPLETED
                        || state.getStatus() == QuestStatus.CLAIMING
                        || state.getStatus() == QuestStatus.CLAIMED)
                .orElse(false);
    }

    public boolean hasClaimed(UUID playerUuid, String questId) {
        return getState(playerUuid, questId)
                .map(state -> state.getStatus() == QuestStatus.CLAIMED)
                .orElse(false);
    }

    public boolean canStart(UUID playerUuid, QuestDefinition quest) {
        if (quest.isRepeatable()) {
            return true;
        }
        return !hasCompleted(playerUuid, quest.getId()) && !hasClaimed(playerUuid, quest.getId());
    }

    public void markCompleted(UUID playerUuid, String questId) throws IOException {
        if (hasClaimed(playerUuid, questId)) {
            throw new IllegalStateException("quest has already been claimed: " + questId);
        }

        Optional<PlayerQuestState> previousState = getState(playerUuid, questId);
        long now = System.currentTimeMillis();
        long completedAt = previousState.map(PlayerQuestState::getCompletedAt).filter(value -> value > 0).orElse(now);
        long claimedAt = getState(playerUuid, questId).map(PlayerQuestState::getClaimedAt).orElse(0L);
        setState(playerUuid, questId, new PlayerQuestState(playerUuid, questId, QuestStatus.COMPLETED, completedAt, claimedAt));
        try {
            save();
        } catch (IOException exception) {
            restoreState(playerUuid, questId, previousState);
            throw exception;
        }
    }

    public void markClaiming(UUID playerUuid, String questId) throws IOException {
        Optional<PlayerQuestState> previousState = getState(playerUuid, questId);
        if (previousState.isPresent() && previousState.get().getStatus() == QuestStatus.CLAIMED) {
            throw new IllegalStateException("quest has already been claimed: " + questId);
        }
        if (previousState.isPresent() && previousState.get().getStatus() == QuestStatus.CLAIMING) {
            throw new IllegalStateException("quest reward claim is already in progress: " + questId);
        }

        long now = System.currentTimeMillis();
        long completedAt = previousState.map(PlayerQuestState::getCompletedAt).filter(value -> value > 0).orElse(now);
        long claimedAt = previousState.map(PlayerQuestState::getClaimedAt).orElse(0L);
        setState(playerUuid, questId, new PlayerQuestState(playerUuid, questId, QuestStatus.CLAIMING, completedAt, claimedAt));
        try {
            save();
        } catch (IOException exception) {
            restoreState(playerUuid, questId, previousState);
            throw exception;
        }
    }

    public void markClaimed(UUID playerUuid, String questId) throws IOException {
        if (hasClaimed(playerUuid, questId)) {
            throw new IllegalStateException("quest has already been claimed: " + questId);
        }

        Optional<PlayerQuestState> previousState = getState(playerUuid, questId);
        long now = System.currentTimeMillis();
        long completedAt = getState(playerUuid, questId).map(PlayerQuestState::getCompletedAt).filter(value -> value > 0).orElse(now);
        setState(playerUuid, questId, new PlayerQuestState(playerUuid, questId, QuestStatus.CLAIMED, completedAt, now));
        try {
            save();
        } catch (IOException exception) {
            restoreState(playerUuid, questId, previousState);
            throw exception;
        }
    }

    public void markStatus(UUID playerUuid, String questId, QuestStatus status) throws IOException {
        switch (status) {
            case COMPLETED -> markCompleted(playerUuid, questId);
            case CLAIMING -> markClaiming(playerUuid, questId);
            case CLAIMED -> markClaimed(playerUuid, questId);
            case NOT_STARTED -> {
                Optional<PlayerQuestState> previousState = getState(playerUuid, questId);
                setState(playerUuid, questId, new PlayerQuestState(playerUuid, questId, QuestStatus.NOT_STARTED, 0L, 0L));
                try {
                    save();
                } catch (IOException exception) {
                    restoreState(playerUuid, questId, previousState);
                    throw exception;
                }
            }
        }
    }

    private void restoreState(UUID playerUuid, String questId, Optional<PlayerQuestState> previousState) {
        Map<String, PlayerQuestState> playerStates = states.get(playerUuid);
        if (previousState.isPresent()) {
            states.computeIfAbsent(playerUuid, ignored -> new LinkedHashMap<>()).put(questId, previousState.get());
            return;
        }

        if (playerStates == null) {
            return;
        }

        playerStates.remove(questId);
        if (playerStates.isEmpty()) {
            states.remove(playerUuid);
        }
    }

    private static String encodePlayerQuestStates(Map<UUID, Map<String, PlayerQuestState>> states) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");

        int playerIndex = 0;
        for (Map.Entry<UUID, Map<String, PlayerQuestState>> playerEntry : states.entrySet()) {
            if (playerIndex > 0) {
                builder.append(",\n");
            }

            appendIndent(builder, 2);
            builder.append('"').append(playerEntry.getKey()).append("\": {\n");

            int questIndex = 0;
            for (PlayerQuestState state : playerEntry.getValue().values()) {
                if (questIndex > 0) {
                    builder.append(",\n");
                }

                appendIndent(builder, 4);
                builder.append('"').append(escapeJson(state.getQuestId())).append("\": {\n");
                appendStringField(builder, "status", state.getStatus().name(), true, 6);
                appendNumberField(builder, "completedAt", state.getCompletedAt(), true, 6);
                appendNumberField(builder, "claimedAt", state.getClaimedAt(), false, 6);
                appendIndent(builder, 4);
                builder.append('}');
                questIndex++;
            }

            builder.append('\n');
            appendIndent(builder, 2);
            builder.append('}');
            playerIndex++;
        }

        builder.append("\n}\n");
        return builder.toString();
    }

    private static Map<UUID, Map<String, PlayerQuestState>> decodePlayerQuestStates(String json) {
        Map<String, Object> root = asObject(new JsonParser(json).parse(), "player quest root");
        Map<UUID, Map<String, PlayerQuestState>> decodedStates = new LinkedHashMap<>();

        for (Map.Entry<String, Object> playerEntry : root.entrySet()) {
            UUID playerUuid = UUID.fromString(playerEntry.getKey());
            Map<String, Object> questStates = asObject(playerEntry.getValue(), "player quest states");
            Map<String, PlayerQuestState> decodedQuestStates = new LinkedHashMap<>();

            for (Map.Entry<String, Object> questEntry : questStates.entrySet()) {
                String questId = questEntry.getKey();
                Map<String, Object> stateValue = asObject(questEntry.getValue(), "quest state");
                String statusValue = asString(stateValue.get("status"), "status");
                Optional<QuestStatus> status = parseStatus(statusValue, playerUuid, questId);
                if (status.isEmpty()) {
                    continue;
                }
                long completedAt = asLong(stateValue.get("completedAt"), "completedAt");
                long claimedAt = asLong(stateValue.get("claimedAt"), "claimedAt");
                decodedQuestStates.put(questId, new PlayerQuestState(playerUuid, questId, status.get(), completedAt, claimedAt));
            }

            decodedStates.put(playerUuid, decodedQuestStates);
        }

        return decodedStates;
    }

    private static Optional<QuestStatus> parseStatus(String statusValue, UUID playerUuid, String questId) {
        try {
            return Optional.of(QuestStatus.valueOf(statusValue));
        } catch (IllegalArgumentException exception) {
            LOGGER.warn(
                    "Unknown player quest status '{}' for player UUID {} quest {}. The entry will be ignored.",
                    statusValue,
                    playerUuid,
                    questId
            );
            return Optional.empty();
        }
    }

    private static void appendStringField(StringBuilder builder, String name, String value, boolean trailingComma, int indent) {
        appendIndent(builder, indent);
        builder.append('"').append(name).append("\": \"").append(escapeJson(value)).append('"');
        appendLineEnding(builder, trailingComma);
    }

    private static void appendNumberField(StringBuilder builder, String name, long value, boolean trailingComma, int indent) {
        appendIndent(builder, indent);
        builder.append('"').append(name).append("\": ").append(value);
        appendLineEnding(builder, trailingComma);
    }

    private static void appendLineEnding(StringBuilder builder, boolean trailingComma) {
        if (trailingComma) {
            builder.append(',');
        }
        builder.append('\n');
    }

    private static void appendIndent(StringBuilder builder, int spaces) {
        builder.append(" ".repeat(spaces));
    }

    private static String escapeJson(String value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            switch (character) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (character < 0x20) {
                        builder.append(String.format("\\u%04x", (int) character));
                    } else {
                        builder.append(character);
                    }
                }
            }
        }
        return builder.toString();
    }

    private static Map<String, Object> asObject(Object value, String fieldName) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> object = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!(entry.getKey() instanceof String key)) {
                    throw new IllegalArgumentException(fieldName + " has a non-string key");
                }
                object.put(key, entry.getValue());
            }
            return object;
        }
        throw new IllegalArgumentException(fieldName + " must be an object");
    }

    private static String asString(Object value, String fieldName) {
        if (value instanceof String string) {
            return string;
        }
        throw new IllegalArgumentException(fieldName + " must be a string");
    }

    private static long asLong(Object value, String fieldName) {
        if (value instanceof Long longValue) {
            return longValue;
        }
        throw new IllegalArgumentException(fieldName + " must be a number");
    }

    public record LoadResult(boolean success, Map<UUID, Map<String, PlayerQuestState>> states, String errorMessage) {
        private static LoadResult success(Map<UUID, Map<String, PlayerQuestState>> states) {
            return new LoadResult(true, copyStates(states), "");
        }

        private static LoadResult failure(Map<UUID, Map<String, PlayerQuestState>> states, String errorMessage) {
            return new LoadResult(false, copyStates(states), errorMessage);
        }

        private static Map<UUID, Map<String, PlayerQuestState>> copyStates(Map<UUID, Map<String, PlayerQuestState>> states) {
            Map<UUID, Map<String, PlayerQuestState>> copiedStates = new LinkedHashMap<>();
            for (Map.Entry<UUID, Map<String, PlayerQuestState>> entry : states.entrySet()) {
                copiedStates.put(entry.getKey(), Map.copyOf(entry.getValue()));
            }
            return Map.copyOf(copiedStates);
        }
    }

    private static final class JsonParser {
        private final String json;
        private int index;

        private JsonParser(String json) {
            this.json = json;
        }

        private Object parse() {
            Object value = parseValue();
            skipWhitespace();
            if (index != json.length()) {
                throw error("unexpected trailing content");
            }
            return value;
        }

        private Object parseValue() {
            skipWhitespace();
            if (index >= json.length()) {
                throw error("unexpected end of input");
            }

            char character = json.charAt(index);
            return switch (character) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't' -> parseLiteral("true", Boolean.TRUE);
                case 'f' -> parseLiteral("false", Boolean.FALSE);
                case 'n' -> parseLiteral("null", null);
                default -> {
                    if (character == '-' || Character.isDigit(character)) {
                        yield parseNumber();
                    }
                    throw error("unexpected character '" + character + "'");
                }
            };
        }

        private Map<String, Object> parseObject() {
            expect('{');
            Map<String, Object> object = new LinkedHashMap<>();
            skipWhitespace();
            if (peek('}')) {
                index++;
                return object;
            }

            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                object.put(key, parseValue());
                skipWhitespace();
                if (peek('}')) {
                    index++;
                    return object;
                }
                expect(',');
            }
        }

        private List<Object> parseArray() {
            expect('[');
            List<Object> array = new ArrayList<>();
            skipWhitespace();
            if (peek(']')) {
                index++;
                return array;
            }

            while (true) {
                array.add(parseValue());
                skipWhitespace();
                if (peek(']')) {
                    index++;
                    return array;
                }
                expect(',');
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder builder = new StringBuilder();
            while (index < json.length()) {
                char character = json.charAt(index++);
                if (character == '"') {
                    return builder.toString();
                }
                if (character != '\\') {
                    builder.append(character);
                    continue;
                }

                if (index >= json.length()) {
                    throw error("unterminated escape sequence");
                }
                char escaped = json.charAt(index++);
                switch (escaped) {
                    case '"' -> builder.append('"');
                    case '\\' -> builder.append('\\');
                    case '/' -> builder.append('/');
                    case 'b' -> builder.append('\b');
                    case 'f' -> builder.append('\f');
                    case 'n' -> builder.append('\n');
                    case 'r' -> builder.append('\r');
                    case 't' -> builder.append('\t');
                    case 'u' -> builder.append(parseUnicodeEscape());
                    default -> throw error("invalid escape sequence");
                }
            }
            throw error("unterminated string");
        }

        private char parseUnicodeEscape() {
            if (index + 4 > json.length()) {
                throw error("invalid unicode escape");
            }
            String hex = json.substring(index, index + 4);
            index += 4;
            try {
                return (char) Integer.parseInt(hex, 16);
            } catch (NumberFormatException exception) {
                throw error("invalid unicode escape");
            }
        }

        private Object parseNumber() {
            int start = index;
            if (peek('-')) {
                index++;
            }
            if (index >= json.length() || !Character.isDigit(json.charAt(index))) {
                throw error("invalid number");
            }
            if (peek('0')) {
                index++;
            } else {
                while (index < json.length() && Character.isDigit(json.charAt(index))) {
                    index++;
                }
            }
            if (index < json.length() && (json.charAt(index) == '.' || json.charAt(index) == 'e' || json.charAt(index) == 'E')) {
                throw error("only integer numbers are supported");
            }
            try {
                return Long.parseLong(json.substring(start, index));
            } catch (NumberFormatException exception) {
                throw error("number is out of range");
            }
        }

        private Object parseLiteral(String literal, Object value) {
            if (!json.startsWith(literal, index)) {
                throw error("invalid literal");
            }
            index += literal.length();
            return value;
        }

        private void skipWhitespace() {
            while (index < json.length() && Character.isWhitespace(json.charAt(index))) {
                index++;
            }
        }

        private void expect(char expected) {
            if (!peek(expected)) {
                throw error("expected '" + expected + "'");
            }
            index++;
        }

        private boolean peek(char expected) {
            return index < json.length() && json.charAt(index) == expected;
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " at character " + index);
        }
    }
}
