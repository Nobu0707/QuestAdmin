package net.nobu0707.questadmin.quest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class QuestStorage {
    private final Path questsPath;
    private List<QuestDefinition> quests = List.of();

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
            List<QuestDefinition> loadedQuests = decodeQuestList(json);
            quests = loadedQuests;
            return LoadResult.success(loadedQuests);
        } catch (IOException | RuntimeException exception) {
            System.err.println("[QuestAdmin] Failed to load quests from " + questsPath + ": " + exception.getMessage());
            return LoadResult.failure(quests, exception.getMessage());
        }
    }

    public void saveQuests(Collection<QuestDefinition> questsToSave) throws IOException {
        Files.createDirectories(questsPath.getParent());

        Path temporaryPath = questsPath.resolveSibling(questsPath.getFileName() + ".tmp");
        Files.writeString(temporaryPath, encodeQuestList(questsToSave), StandardCharsets.UTF_8);
        Files.move(temporaryPath, questsPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        quests = List.copyOf(questsToSave);
    }

    private void createSampleQuestFile() {
        QuestDefinition sampleQuest = QuestDefinition.createSample(System.currentTimeMillis());
        try {
            saveQuests(List.of(sampleQuest));
        } catch (IOException exception) {
            System.err.println("[QuestAdmin] Failed to create sample quest file at " + questsPath + ": " + exception.getMessage());
            quests = List.of(sampleQuest);
        }
    }

    public record LoadResult(boolean success, List<QuestDefinition> quests, String errorMessage) {
        private static LoadResult success(List<QuestDefinition> quests) {
            return new LoadResult(true, List.copyOf(quests), "");
        }

        private static LoadResult failure(List<QuestDefinition> quests, String errorMessage) {
            return new LoadResult(false, List.copyOf(quests), errorMessage);
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

    private static List<QuestDefinition> decodeQuestList(String json) {
        Object value = new JsonParser(json).parse();
        if (!(value instanceof List<?> values)) {
            throw new IllegalArgumentException("quest root must be a JSON array");
        }

        List<QuestDefinition> decodedQuests = new ArrayList<>();
        for (Object item : values) {
            decodedQuests.add(decodeQuest(asObject(item, "quest")));
        }
        return List.copyOf(decodedQuests);
    }

    private static QuestDefinition decodeQuest(Map<String, Object> value) {
        Map<String, Object> requirement = asObject(value.get("requirement"), "requirement");
        Map<String, Object> reward = asObject(value.get("reward"), "reward");

        return new QuestDefinition(
                asString(value.get("id"), "id"),
                asString(value.get("title"), "title"),
                asString(value.get("description"), "description"),
                QuestType.valueOf(asString(value.get("type"), "type")),
                new QuestRequirement(
                        asString(requirement.get("itemId"), "requirement.itemId"),
                        Math.toIntExact(asLong(requirement.get("amount"), "requirement.amount"))
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

    private static boolean asBoolean(Object value, String fieldName) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        throw new IllegalArgumentException(fieldName + " must be a boolean");
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
