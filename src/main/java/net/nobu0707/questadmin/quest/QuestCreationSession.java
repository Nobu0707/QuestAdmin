package net.nobu0707.questadmin.quest;

import java.util.Optional;

public final class QuestCreationSession {
    private QuestCreationStep step = QuestCreationStep.QUEST_ID;
    private String questId = "";
    private String title = "";
    private String description = "";
    private String itemId = "";
    private int amount;
    private long rewardMoney;
    private boolean repeatable;
    private boolean enabled;

    public QuestCreationStep getStep() {
        return step;
    }

    public StepResult accept(String input, QuestStorage storage) {
        String value = input.trim();
        if (value.equalsIgnoreCase("cancel")) {
            return StepResult.cancelledResult();
        }

        StepResult validationResult = switch (step) {
            case QUEST_ID -> acceptQuestId(value, storage);
            case TITLE -> acceptTitle(value);
            case DESCRIPTION -> acceptDescription(input);
            case ITEM_ID -> acceptItemId(value);
            case AMOUNT -> acceptAmount(value);
            case REWARD_MONEY -> acceptRewardMoney(value);
            case REPEATABLE -> acceptRepeatable(value);
            case ENABLED -> acceptEnabled(value);
        };

        if (!validationResult.success()) {
            return validationResult;
        }

        QuestCreationStep nextStep = step.next();
        if (nextStep == null) {
            return StepResult.completed(createQuest());
        }

        step = nextStep;
        return StepResult.next(nextStep.getPrompt());
    }

    private StepResult acceptQuestId(String value, QuestStorage storage) {
        QuestValidationResult validationResult = QuestValidator.validateQuestIdForCreate(value, storage.getQuests());
        if (!validationResult.isValid()) {
            return StepResult.retry(validationResult.firstMessage());
        }

        questId = value;
        return StepResult.accepted();
    }

    private StepResult acceptTitle(String value) {
        QuestValidationResult validationResult = QuestValidator.validateTitle(value);
        if (!validationResult.isValid()) {
            return StepResult.retry(validationResult.firstMessage());
        }

        title = value;
        return StepResult.accepted();
    }

    private StepResult acceptDescription(String input) {
        String value = input.trim();
        description = value.equals("-") ? "" : value;
        QuestValidationResult validationResult = QuestValidator.validateDescription(description);
        if (!validationResult.isValid()) {
            return StepResult.retry(validationResult.firstMessage());
        }
        return StepResult.accepted();
    }

    private StepResult acceptItemId(String value) {
        QuestValidationResult validationResult = QuestValidator.validateItemId(value);
        if (!validationResult.isValid()) {
            return StepResult.retry(validationResult.firstMessage());
        }

        itemId = value;
        return StepResult.accepted();
    }

    private StepResult acceptAmount(String value) {
        Optional<Integer> parsedAmount = parseInt(value);
        if (parsedAmount.isEmpty()) {
            return StepResult.retry("QuestAdmin: 必要個数は整数で入力してください。");
        }

        QuestValidationResult validationResult = QuestValidator.validateAmount(parsedAmount.get());
        if (!validationResult.isValid()) {
            return StepResult.retry(validationResult.firstMessage());
        }

        amount = parsedAmount.get();
        return StepResult.accepted();
    }

    private StepResult acceptRewardMoney(String value) {
        Optional<Long> parsedRewardMoney = parseLong(value);
        if (parsedRewardMoney.isEmpty()) {
            return StepResult.retry("QuestAdmin: 報酬金額は整数で入力してください。");
        }

        QuestValidationResult validationResult = QuestValidator.validateRewardMoney(parsedRewardMoney.get());
        if (!validationResult.isValid()) {
            return StepResult.retry(validationResult.firstMessage());
        }

        rewardMoney = parsedRewardMoney.get();
        return StepResult.accepted();
    }

    private StepResult acceptRepeatable(String value) {
        Optional<Boolean> parsedValue = parseBoolean(value);
        if (parsedValue.isEmpty()) {
            return StepResult.retry("QuestAdmin: true または false を入力してください。");
        }

        QuestValidationResult validationResult = QuestValidator.validateRepeatable(parsedValue.get());
        if (!validationResult.isValid()) {
            return StepResult.retry(validationResult.firstMessage());
        }

        repeatable = parsedValue.get();
        return StepResult.accepted();
    }

    private StepResult acceptEnabled(String value) {
        Optional<Boolean> parsedValue = parseBoolean(value);
        if (parsedValue.isEmpty()) {
            return StepResult.retry("QuestAdmin: true または false を入力してください。");
        }

        enabled = parsedValue.get();
        return StepResult.accepted();
    }

    private QuestDefinition createQuest() {
        long now = System.currentTimeMillis();
        return new QuestDefinition(
                questId,
                title,
                description,
                QuestType.ITEM_DELIVERY,
                new QuestRequirement(itemId, amount),
                new QuestReward(rewardMoney),
                repeatable,
                enabled,
                now,
                now
        );
    }

    private static Optional<Integer> parseInt(String value) {
        try {
            return Optional.of(Integer.parseInt(value));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private static Optional<Long> parseLong(String value) {
        try {
            return Optional.of(Long.parseLong(value));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private static Optional<Boolean> parseBoolean(String value) {
        if ("true".equals(value)) {
            return Optional.of(true);
        }
        if ("false".equals(value)) {
            return Optional.of(false);
        }
        return Optional.empty();
    }

    public record StepResult(boolean success, boolean cancelled, boolean completed, String message, QuestDefinition quest) {
        private static StepResult accepted() {
            return new StepResult(true, false, false, "", null);
        }

        private static StepResult next(String message) {
            return new StepResult(true, false, false, message, null);
        }

        private static StepResult retry(String message) {
            return new StepResult(false, false, false, message, null);
        }

        private static StepResult completed(QuestDefinition quest) {
            return new StepResult(true, false, true, "", quest);
        }

        private static StepResult cancelledResult() {
            return new StepResult(true, true, false, "", null);
        }
    }
}
