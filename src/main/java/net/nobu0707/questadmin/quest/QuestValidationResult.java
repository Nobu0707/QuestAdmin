package net.nobu0707.questadmin.quest;

import java.util.List;

public record QuestValidationResult(List<QuestValidationError> errors) {
    public static QuestValidationResult success() {
        return new QuestValidationResult(List.of());
    }

    public static QuestValidationResult error(String message) {
        return new QuestValidationResult(List.of(new QuestValidationError(message)));
    }

    public boolean isValid() {
        return errors.isEmpty();
    }

    public String firstMessage() {
        return isValid() ? "" : errors.get(0).message();
    }

    public String joinedMessages() {
        return String.join(
                "\n",
                errors.stream()
                        .map(QuestValidationError::message)
                        .toList()
        );
    }
}
