package net.nobu0707.questadmin.quest;

import java.util.Optional;

public final class QuestEditSession {
    private final QuestDefinition originalQuest;
    private QuestEditStep step = QuestEditStep.TITLE;
    private String title;
    private String description;
    private String itemId;
    private int amount;
    private long rewardMoney;
    private boolean repeatable;
    private boolean enabled;

    public QuestEditSession(QuestDefinition quest) {
        this.originalQuest = quest;
        this.title = quest.getTitle();
        this.description = quest.getDescription();
        this.itemId = quest.getRequirement().getItemId();
        this.amount = quest.getRequirement().getAmount();
        this.rewardMoney = quest.getReward().getMoney();
        this.repeatable = quest.isRepeatable();
        this.enabled = quest.isEnabled();
    }

    public String getQuestId() {
        return originalQuest.getId();
    }

    public QuestEditStep getStep() {
        return step;
    }

    public String getPrompt() {
        return switch (step) {
            case TITLE -> "タイトルを入力してください。現在値: " + title;
            case DESCRIPTION -> "説明文を入力してください。現在値: " + description;
            case ITEM_ID -> "必要アイテムIDを入力してください。現在値: " + itemId;
            case AMOUNT -> "必要個数を入力してください。現在値: " + amount;
            case REWARD_MONEY -> "報酬金額を入力してください。現在値: " + rewardMoney;
            case REPEATABLE -> "repeatable は現在未対応です。false を入力してください。現在値: " + repeatable;
            case ENABLED -> "有効化しますか？ true / false 現在値: " + enabled;
            case CONFIRM -> "保存する場合は true、キャンセルする場合は false を入力してください。";
        };
    }

    public StepResult accept(String input) {
        String value = input.trim();
        if (value.equalsIgnoreCase("cancel")) {
            return StepResult.cancelledResult();
        }

        if (step == QuestEditStep.CONFIRM) {
            return acceptConfirm(value);
        }

        StepResult validationResult = switch (step) {
            case TITLE -> acceptTitle(value);
            case DESCRIPTION -> acceptDescription(input);
            case ITEM_ID -> acceptItemId(value);
            case AMOUNT -> acceptAmount(value);
            case REWARD_MONEY -> acceptRewardMoney(value);
            case REPEATABLE -> acceptRepeatable(value);
            case ENABLED -> acceptEnabled(value);
            case CONFIRM -> throw new IllegalStateException("confirm step is handled separately");
        };

        if (!validationResult.success()) {
            return validationResult;
        }

        step = step.next();
        if (step == QuestEditStep.CONFIRM) {
            return StepResult.next(createSummary());
        }
        return StepResult.next(getPrompt());
    }

    private StepResult acceptTitle(String value) {
        if (isKeepValue(value)) {
            return StepResult.accepted();
        }

        QuestValidationResult validationResult = QuestValidator.validateTitle(value);
        if (!validationResult.isValid()) {
            return StepResult.retry(validationResult.firstMessage());
        }

        title = value;
        return StepResult.accepted();
    }

    private StepResult acceptDescription(String input) {
        String value = input.trim();
        if (isKeepValue(value)) {
            return StepResult.accepted();
        }

        QuestValidationResult validationResult = QuestValidator.validateDescription(value);
        if (!validationResult.isValid()) {
            return StepResult.retry(validationResult.firstMessage());
        }

        description = value;
        return StepResult.accepted();
    }

    private StepResult acceptItemId(String value) {
        if (isKeepValue(value)) {
            return StepResult.accepted();
        }

        QuestValidationResult validationResult = QuestValidator.validateItemId(value);
        if (!validationResult.isValid()) {
            return StepResult.retry(validationResult.firstMessage());
        }

        itemId = value;
        return StepResult.accepted();
    }

    private StepResult acceptAmount(String value) {
        if (isKeepValue(value)) {
            return StepResult.accepted();
        }

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
        if (isKeepValue(value)) {
            return StepResult.accepted();
        }

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
        if (isKeepValue(value)) {
            QuestValidationResult validationResult = QuestValidator.validateRepeatable(repeatable);
            if (!validationResult.isValid()) {
                return StepResult.retry(validationResult.firstMessage());
            }
            return StepResult.accepted();
        }

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
        if (isKeepValue(value)) {
            return StepResult.accepted();
        }

        Optional<Boolean> parsedValue = parseBoolean(value);
        if (parsedValue.isEmpty()) {
            return StepResult.retry("QuestAdmin: true または false を入力してください。");
        }

        enabled = parsedValue.get();
        return StepResult.accepted();
    }

    private StepResult acceptConfirm(String value) {
        Optional<Boolean> parsedValue = parseBoolean(value);
        if (parsedValue.isEmpty()) {
            return StepResult.retry("QuestAdmin: 保存する場合は true、キャンセルする場合は false を入力してください。");
        }

        if (!parsedValue.get()) {
            return StepResult.cancelledResult();
        }

        return StepResult.completed(createUpdatedQuest());
    }

    private QuestDefinition createUpdatedQuest() {
        return new QuestDefinition(
                originalQuest.getId(),
                title,
                description,
                originalQuest.getType(),
                new QuestRequirement(itemId, amount),
                new QuestReward(rewardMoney),
                repeatable,
                enabled,
                originalQuest.getCreatedAt(),
                System.currentTimeMillis()
        );
    }

    private String createSummary() {
        return "QuestAdmin: 以下の内容で更新します。\n"
                + "ID: " + originalQuest.getId() + "\n"
                + "Title: " + title + "\n"
                + "Description: " + description + "\n"
                + "Item: " + itemId + " x" + amount + "\n"
                + "Reward: " + rewardMoney + "\n"
                + "Repeatable: " + repeatable + "\n"
                + "Enabled: " + enabled;
    }

    private static boolean isKeepValue(String value) {
        return value.isBlank() || value.equals("-");
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
