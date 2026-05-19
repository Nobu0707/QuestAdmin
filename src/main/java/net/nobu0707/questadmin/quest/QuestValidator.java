package net.nobu0707.questadmin.quest;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

public final class QuestValidator {
    public static final int TITLE_MAX_LENGTH = 64;
    public static final int DESCRIPTION_MAX_LENGTH = 256;
    public static final int MAX_AMOUNT = 999_999;
    public static final long MAX_REWARD_MONEY = 999_999_999L;
    public static final String REPEATABLE_UNSUPPORTED_MESSAGE =
            "QuestAdmin: 現在、繰り返しクエストは未対応です。repeatable=false を使用してください。";

    private static final Pattern QUEST_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");

    private QuestValidator() {
    }

    public static QuestValidationResult validateQuestList(Collection<QuestDefinition> quests) {
        List<QuestValidationError> errors = new ArrayList<>();
        Set<String> questIds = new HashSet<>();

        for (QuestDefinition quest : quests) {
            QuestValidationResult questResult = validateDefinition(quest);
            errors.addAll(questResult.errors());

            if (quest != null && !quest.getId().isBlank() && !questIds.add(quest.getId())) {
                errors.add(new QuestValidationError("QuestAdmin: questId が重複しています: " + quest.getId()));
            }
        }

        return new QuestValidationResult(List.copyOf(errors));
    }

    public static QuestValidationResult validateForLoad(QuestDefinition quest, Collection<String> existingQuestIds) {
        List<QuestValidationError> errors = new ArrayList<>(validateDefinition(quest).errors());
        if (quest != null && existingQuestIds.contains(quest.getId())) {
            errors.add(new QuestValidationError("QuestAdmin: questId が重複しています: " + quest.getId()));
        }
        return new QuestValidationResult(List.copyOf(errors));
    }

    public static QuestValidationResult validateDefinition(QuestDefinition quest) {
        List<QuestValidationError> errors = new ArrayList<>();
        if (quest == null) {
            errors.add(new QuestValidationError("QuestAdmin: クエスト定義が不正です。"));
            return new QuestValidationResult(List.copyOf(errors));
        }

        errors.addAll(validateQuestId(quest.getId()).errors());
        errors.addAll(validateTitle(quest.getTitle()).errors());
        errors.addAll(validateDescription(quest.getDescription()).errors());
        errors.addAll(validateType(quest.getType()).errors());

        if (quest.getRequirement() == null) {
            errors.add(new QuestValidationError("QuestAdmin: requirement が不正です。"));
        } else {
            errors.addAll(validateItemId(quest.getRequirement().getItemId()).errors());
            errors.addAll(validateAmount(quest.getRequirement().getAmount()).errors());
        }

        if (quest.getReward() == null) {
            errors.add(new QuestValidationError("QuestAdmin: reward が不正です。"));
        } else {
            errors.addAll(validateRewardMoney(quest.getReward().getMoney()).errors());
        }

        errors.addAll(validateRepeatable(quest.isRepeatable()).errors());
        return new QuestValidationResult(List.copyOf(errors));
    }

    public static QuestValidationResult validateQuestIdForCreate(String questId, Collection<QuestDefinition> existingQuests) {
        QuestValidationResult questIdResult = validateQuestId(questId);
        if (!questIdResult.isValid()) {
            return questIdResult;
        }

        boolean duplicate = existingQuests.stream()
                .anyMatch(quest -> quest.getId().equals(questId));
        if (duplicate) {
            return QuestValidationResult.error("QuestAdmin: questId が重複しています: " + questId);
        }

        return QuestValidationResult.success();
    }

    public static QuestValidationResult validateQuestId(String questId) {
        if (questId == null || questId.isBlank() || !QUEST_ID_PATTERN.matcher(questId).matches()) {
            return QuestValidationResult.error("QuestAdmin: questId が不正です。半角英数字、_、- を使用してください。");
        }
        return QuestValidationResult.success();
    }

    public static QuestValidationResult validateTitle(String title) {
        if (title == null || title.isBlank()) {
            return QuestValidationResult.error("QuestAdmin: title は空にできません。");
        }
        if (title.length() > TITLE_MAX_LENGTH) {
            return QuestValidationResult.error("QuestAdmin: title が長すぎます。最大 " + TITLE_MAX_LENGTH + " 文字です。");
        }
        return QuestValidationResult.success();
    }

    public static QuestValidationResult validateDescription(String description) {
        if (description == null) {
            return QuestValidationResult.success();
        }
        if (description.length() > DESCRIPTION_MAX_LENGTH) {
            return QuestValidationResult.error("QuestAdmin: description が長すぎます。最大 " + DESCRIPTION_MAX_LENGTH + " 文字です。");
        }
        return QuestValidationResult.success();
    }

    public static QuestValidationResult validateType(QuestType type) {
        if (type != QuestType.ITEM_DELIVERY) {
            return QuestValidationResult.error("QuestAdmin: type は ITEM_DELIVERY のみ対応です。");
        }
        return QuestValidationResult.success();
    }

    public static QuestValidationResult validateItemId(String itemId) {
        if (resolveItem(itemId).isEmpty()) {
            return QuestValidationResult.error("QuestAdmin: itemId が不正です。Minecraftに存在するアイテムIDを指定してください: " + itemId);
        }
        return QuestValidationResult.success();
    }

    public static QuestValidationResult validateAmount(int amount) {
        if (amount < 1 || amount > MAX_AMOUNT) {
            return QuestValidationResult.error("QuestAdmin: 必要個数は 1 以上 " + MAX_AMOUNT + " 以下で入力してください。");
        }
        return QuestValidationResult.success();
    }

    public static QuestValidationResult validateRewardMoney(long rewardMoney) {
        if (rewardMoney < 0 || rewardMoney > MAX_REWARD_MONEY) {
            return QuestValidationResult.error("QuestAdmin: 報酬金額は 0 以上 " + MAX_REWARD_MONEY + " 以下で入力してください。");
        }
        return QuestValidationResult.success();
    }

    public static QuestValidationResult validateRepeatable(boolean repeatable) {
        if (repeatable) {
            return QuestValidationResult.error(REPEATABLE_UNSUPPORTED_MESSAGE);
        }
        return QuestValidationResult.success();
    }

    public static Optional<Item> resolveItem(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return Optional.empty();
        }

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
}
