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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class QuestStorage {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final Path questsPath;
    private List<QuestDefinition> quests = List.of();
    private Map<String, QuestDefinition> questById = Map.of();

    public QuestStorage(Path questsPath) {
        this.questsPath = questsPath;
    }

    public static QuestStorage createDefault() {
        return new QuestStorage(Path.of("config", "questadmin", "quests.json"));
    }

    public static QuestStorage createDefault(Path configDirectory) {
        return new QuestStorage(configDirectory.resolve("questadmin").resolve("quests.json"));
    }

    public void initialize() {
        if (Files.notExists(questsPath)) {
            createSampleQuestFile();
            return;
        }

        quests = loadQuests();
    }

    public List<QuestDefinition> getQuests() {
        return Collections.unmodifiableList(quests);
    }

    public Optional<QuestDefinition> findById(String questId) {
        return Optional.ofNullable(questById.get(questId));
    }

    public boolean exists(String questId) {
        return questById.containsKey(questId);
    }

    public Path getQuestsPath() {
        return questsPath;
    }

    public List<QuestDefinition> loadQuests() {
        LoadResult result = reloadQuests();
        return result.quests();
    }

    public LoadResult reloadQuests() {
        try {
            String json = Files.readString(questsPath, StandardCharsets.UTF_8);
            DecodedQuestList decodedQuestList = decodeQuestList(json);
            quests = decodedQuestList.quests();
            rebuildIndex();
            return LoadResult.success(decodedQuestList.quests(), decodedQuestList.skippedCount());
        } catch (IOException | RuntimeException exception) {
            LOGGER.error("Failed to load quests from {}.", questsPath, exception);
            return LoadResult.failure(quests, exception.getMessage());
        }
    }

    public void saveQuests(Collection<QuestDefinition> questsToSave) throws IOException {
        QuestValidationResult validationResult = QuestValidator.validateQuestList(questsToSave);
        if (!validationResult.isValid()) {
            throw new IllegalArgumentException(validationResult.joinedMessages());
        }

        StorageFileUtil.writeStringSafely(questsPath, encodeQuestList(questsToSave), LOGGER);
        quests = List.copyOf(questsToSave);
        rebuildIndex();
    }

    private void createSampleQuestFile() {
        QuestDefinition sampleQuest = QuestDefinition.createSample(System.currentTimeMillis());
        try {
            saveQuests(List.of(sampleQuest));
        } catch (IOException exception) {
            LOGGER.error("Failed to create sample quest file at {}.", questsPath, exception);
            quests = List.of(sampleQuest);
            rebuildIndex();
        }
    }

    private void rebuildIndex() {
        Map<String, QuestDefinition> rebuiltIndex = new LinkedHashMap<>();
        for (QuestDefinition quest : quests) {
            rebuiltIndex.put(quest.getId(), quest);
        }
        questById = Map.copyOf(rebuiltIndex);
    }

    public record LoadResult(boolean success, List<QuestDefinition> quests, String errorMessage, int skippedCount) {
        private static LoadResult success(List<QuestDefinition> quests, int skippedCount) {
            return new LoadResult(true, List.copyOf(quests), "", skippedCount);
        }

        private static LoadResult failure(List<QuestDefinition> quests, String errorMessage) {
            return new LoadResult(false, List.copyOf(quests), errorMessage, 0);
        }
    }

    private static String encodeQuestList(Collection<QuestDefinition> questList) {
        StringBuilder builder = new StringBuilder();
        builder.append("[\n");

        int index = 0;
        for (QuestDefinition quest : questList) {
            if (index > 0) {
                builder.append(",\n");
            }

            builder.append("  {\n");
            appendStringField(builder, "id", quest.getId(), true, 4);
            appendStringField(builder, "title", quest.getTitle(), true, 4);
            appendStringField(builder, "description", quest.getDescription(), true, 4);
            appendStringField(builder, "type", quest.getType().name(), true, 4);
            builder.append("    \"requirement\": {\n");
            appendStringField(builder, "itemId", quest.getRequirement().getItemId(), true, 6);
            appendNumberField(builder, "amount", quest.getRequirement().getAmount(), false, 6);
            builder.append("    },\n");
            builder.append("    \"reward\": {\n");
            appendNumberField(builder, "money", quest.getReward().getMoney(), false, 6);
            builder.append("    },\n");
            appendBooleanField(builder, "repeatable", quest.isRepeatable(), true, 4);
            appendBooleanField(builder, "enabled", quest.isEnabled(), true, 4);
            appendNumberField(builder, "createdAt", quest.getCreatedAt(), true, 4);
            appendNumberField(builder, "updatedAt", quest.getUpdatedAt(), false, 4);
            builder.append("  }");
            index++;
        }

        builder.append("\n]\n");
        return builder.toString();
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

    private static void appendBooleanField(StringBuilder builder, String name, boolean value, boolean trailingComma, int indent) {
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

    private static DecodedQuestList decodeQuestList(String json) {
        Object value = new JsonParser(json).parse();
        if (!(value instanceof List<?> values)) {
            throw new IllegalArgumentException("quests.json のルートはJSON配列で指定してください。");
        }

        List<QuestDefinition> decodedQuests = new ArrayList<>();
        Set<String> questIds = new HashSet<>();
        int skippedCount = 0;

        for (int index = 0; index < values.size(); index++) {
            Object item = values.get(index);
            QuestDefinition quest;
            try {
                quest = decodeQuest(asObject(item, "quest"));
            } catch (RuntimeException exception) {
                skippedCount++;
                LOGGER.warn(
                        "QuestAdmin: quests.json の {} 番目のクエストをスキップしました: {}",
                        index + 1,
                        exception.getMessage()
                );
                continue;
            }

            QuestValidationResult validationResult = QuestValidator.validateForLoad(quest, questIds);
            if (!validationResult.isValid()) {
                skippedCount++;
                LOGGER.warn(
                        "QuestAdmin: quests.json の {} 番目のクエストをスキップしました: {}",
                        index + 1,
                        validationResult.joinedMessages()
                );
                continue;
            }

            decodedQuests.add(quest);
            questIds.add(quest.getId());
        }
        return new DecodedQuestList(List.copyOf(decodedQuests), skippedCount);
    }

    private static QuestDefinition decodeQuest(Map<String, Object> value) {
        Map<String, Object> requirement = asObject(value.get("requirement"), "requirement");
        Map<String, Object> reward = asObject(value.get("reward"), "reward");

        return new QuestDefinition(
                asString(value.get("id"), "id"),
                asString(value.get("title"), "title"),
                asOptionalString(value.get("description")),
                asQuestType(value.get("type")),
                new QuestRequirement(
                        asString(requirement.get("itemId"), "requirement.itemId"),
                        asInt(requirement.get("amount"), "requirement.amount")
                ),
                new QuestReward(asLong(reward.get("money"), "reward.money")),
                asBoolean(value.get("repeatable"), "repeatable"),
                asBoolean(value.get("enabled"), "enabled"),
                asLong(value.get("createdAt"), "createdAt"),
                asLong(value.get("updatedAt"), "updatedAt")
        );
    }

    private static Map<String, Object> asObject(Object value, String fieldName) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> object = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!(entry.getKey() instanceof String key)) {
                    throw new IllegalArgumentException(fieldName + " に文字列ではないキーがあります。");
                }
                object.put(key, entry.getValue());
            }
            return object;
        }
        throw new IllegalArgumentException(fieldName + " はJSONオブジェクトで指定してください。");
    }

    private static String asString(Object value, String fieldName) {
        if (value instanceof String string) {
            return string;
        }
        throw new IllegalArgumentException(fieldName + " は文字列で指定してください。");
    }

    private static String asOptionalString(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String string) {
            return string;
        }
        throw new IllegalArgumentException("description は文字列で指定してください。");
    }

    private static QuestType asQuestType(Object value) {
        String typeName = asString(value, "type");
        if (!QuestType.ITEM_DELIVERY.name().equals(typeName)) {
            throw new IllegalArgumentException("QuestAdmin: type は ITEM_DELIVERY のみ対応です: " + typeName);
        }
        return QuestType.ITEM_DELIVERY;
    }

    private static int asInt(Object value, String fieldName) {
        long longValue = asLong(value, fieldName);
        if (longValue < Integer.MIN_VALUE || longValue > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(fieldName + " はint範囲内の整数で指定してください。");
        }
        return (int) longValue;
    }

    private static long asLong(Object value, String fieldName) {
        if (value instanceof Long longValue) {
            return longValue;
        }
        throw new IllegalArgumentException(fieldName + " は整数で指定してください。");
    }

    private static boolean asBoolean(Object value, String fieldName) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        throw new IllegalArgumentException(fieldName + " は true または false で指定してください。");
    }

    private record DecodedQuestList(List<QuestDefinition> quests, int skippedCount) {
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
