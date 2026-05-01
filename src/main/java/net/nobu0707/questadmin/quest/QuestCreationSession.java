package net.nobu0707.questadmin.quest;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Optional;
import java.util.regex.Pattern;

public final class QuestCreationSession {
    private static final Pattern QUEST_ID_PATTERN = Pattern.compile("[A-Za-z0-9_-]+");
    private static final int TITLE_MAX_LENGTH = 128;
    private static final int DESCRIPTION_MAX_LENGTH = 512;
    private static final int MAX_AMOUNT = 999_999;
    private static final long MAX_REWARD_MONEY = 999_999_999L;

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
        if (value.isBlank() || !QUEST_ID_PATTERN.matcher(value).matches()) {
            return StepResult.retry("QuestAdmin: questId が不正です。半角英数字、_、- を使用してください。");
        }

        if (storage.getQuests().stream().anyMatch(quest -> quest.getId().equals(value))) {
            return StepResult.retry("QuestAdmin: 既に同じquestIdのクエストが存在します: " + value);
        }

        questId = value;
        return StepResult.accepted();
    }

    private StepResult acceptTitle(String value) {
        if (value.isBlank()) {
            return StepResult.retry("QuestAdmin: title は空にできません。");
        }

        if (value.length() > TITLE_MAX_LENGTH) {
            return StepResult.retry("QuestAdmin: title が長すぎます。最大 " + TITLE_MAX_LENGTH + " 文字です。");
        }

        title = value;
        return StepResult.accepted();
    }

    private StepResult acceptDescription(String input) {
        String value = input.trim();
        description = value.equals("-") ? "" : value;
        if (description.length() > DESCRIPTION_MAX_LENGTH) {
            return StepResult.retry("QuestAdmin: description が長すぎます。最大 " + DESCRIPTION_MAX_LENGTH + " 文字です。");
        }
        return StepResult.accepted();
    }

    private StepResult acceptItemId(String value) {
        if (resolveItem(value).isEmpty()) {
            return StepResult.retry("QuestAdmin: アイテムIDが見つかりません: " + value);
        }

        itemId = value;
        return StepResult.accepted();
    }

    private StepResult acceptAmount(String value) {
        Optional<Integer> parsedAmount = parseInt(value);
        if (parsedAmount.isEmpty() || parsedAmount.get() < 1 || parsedAmount.get() > MAX_AMOUNT) {
            return StepResult.retry("QuestAdmin: 必要個数は 1 以上 " + MAX_AMOUNT + " 以下で入力してください。");
        }

        amount = parsedAmount.get();
        return StepResult.accepted();
    }

    private StepResult acceptRewardMoney(String value) {
        Optional<Long> parsedRewardMoney = parseLong(value);
        if (parsedRewardMoney.isEmpty() || parsedRewardMoney.get() < 0 || parsedRewardMoney.get() > MAX_REWARD_MONEY) {
            return StepResult.retry("QuestAdmin: 報酬金額は 0 以上 " + MAX_REWARD_MONEY + " 以下で入力してください。");
        }

        rewardMoney = parsedRewardMoney.get();
        return StepResult.accepted();
    }

    private StepResult acceptRepeatable(String value) {
        Optional<Boolean> parsedValue = parseBoolean(value);
        if (parsedValue.isEmpty()) {
            return StepResult.retry("QuestAdmin: true または false を入力してください。");
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

    private static Optional<Item> resolveItem(String itemId) {
        ResourceLocation resourceLocation = ResourceLocation.tryParse(itemId);
        if (resourceLocation == null) {
            return Optional.empty();
        }

        Optional<Item> item = BuiltInRegistries.ITEM.getOptional(resourceLocation);
        if (item.isEmpty() || item.get() == Items.AIR) {
            return Optional.empty();
        }
        return item;
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
