package net.nobu0707.questadmin.quest;

import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class QuestCompletionService {
    private final QuestStorage questStorage;
    private final PlayerQuestStorage playerQuestStorage;

    public QuestCompletionService(QuestStorage questStorage, PlayerQuestStorage playerQuestStorage) {
        this.questStorage = questStorage;
        this.playerQuestStorage = playerQuestStorage;
    }

    public CompletionResult completeItemDeliveryQuest(ServerPlayer player, String questId) {
        QuestDefinition quest = findQuestById(questId).orElse(null);
        if (quest == null) {
            return CompletionResult.failure("QuestAdmin: 存在しないクエストIDです: " + questId);
        }

        if (!quest.isEnabled()) {
            return CompletionResult.failure("QuestAdmin: このクエストは現在無効です。");
        }

        if (quest.getType() != QuestType.ITEM_DELIVERY) {
            return CompletionResult.failure("QuestAdmin: このクエスト種別はまだ完了処理に対応していません。");
        }

        Optional<PlayerQuestState> currentState = playerQuestStorage.getState(player.getUUID(), quest.getId());
        if (currentState.isPresent() && currentState.get().getStatus() == QuestStatus.CLAIMING) {
            return CompletionResult.failure("QuestAdmin: 報酬受け取り処理中、または支払い結果の確認が必要です。管理者へ連絡してください。");
        }

        if (!quest.isRepeatable() && playerQuestStorage.hasCompleted(player.getUUID(), quest.getId())) {
            return CompletionResult.failure("QuestAdmin: このクエストは既に完了済みです。");
        }

        QuestValidationResult amountValidation = QuestValidator.validateAmount(quest.getRequirement().getAmount());
        if (!amountValidation.isValid()) {
            return CompletionResult.failure("QuestAdmin: クエスト定義の必要数が不正です。");
        }

        Item item = resolveItem(quest.getRequirement().getItemId()).orElse(null);
        if (item == null) {
            return CompletionResult.failure("QuestAdmin: クエスト定義のアイテムIDが不正です: " + quest.getRequirement().getItemId());
        }

        int requiredAmount = quest.getRequirement().getAmount();
        int ownedAmount = countItem(player, item);
        if (ownedAmount < requiredAmount) {
            return CompletionResult.failure("QuestAdmin: 必要アイテムが足りません。必要: "
                    + quest.getRequirement().getItemId() + " x" + requiredAmount + " / 所持: " + ownedAmount);
        }

        InventorySnapshot snapshot = InventorySnapshot.capture(player);
        consumeItem(player, item, requiredAmount);
        try {
            playerQuestStorage.markCompleted(player.getUUID(), quest.getId());
        } catch (IOException | IllegalStateException exception) {
            snapshot.restore(player);
            return CompletionResult.failure("QuestAdmin: クエスト状態の保存に失敗したため、アイテム消費を取り消しました: " + exception.getMessage());
        }

        return CompletionResult.success(quest);
    }

    public Optional<QuestDefinition> findQuestById(String questId) {
        return questStorage.getQuests().stream()
                .filter(quest -> quest.getId().equals(questId))
                .findFirst();
    }

    public int countItem(ServerPlayer player, Item item) {
        int count = 0;
        count += countItem(player.getInventory().items, item);
        count += countItem(player.getInventory().offhand, item);
        count += countItem(player.getInventory().armor, item);
        return count;
    }

    public void consumeItem(ServerPlayer player, Item item, int amount) {
        int remaining = amount;
        remaining = consumeItem(player.getInventory().items, item, remaining);
        remaining = consumeItem(player.getInventory().offhand, item, remaining);
        consumeItem(player.getInventory().armor, item, remaining);
        player.getInventory().setChanged();
    }

    private Optional<Item> resolveItem(String itemId) {
        return QuestValidator.resolveItem(itemId);
    }

    private static int countItem(NonNullList<ItemStack> stacks, Item item) {
        int count = 0;
        for (ItemStack stack : stacks) {
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static int consumeItem(NonNullList<ItemStack> stacks, Item item, int amount) {
        int remaining = amount;
        for (ItemStack stack : stacks) {
            if (remaining <= 0) {
                return 0;
            }
            if (!stack.is(item)) {
                continue;
            }

            int consumed = Math.min(stack.getCount(), remaining);
            stack.shrink(consumed);
            remaining -= consumed;
        }
        return remaining;
    }

    public record CompletionResult(boolean success, QuestDefinition quest, String message) {
        private static CompletionResult success(QuestDefinition quest) {
            return new CompletionResult(true, quest, "");
        }

        private static CompletionResult failure(String message) {
            return new CompletionResult(false, null, message);
        }
    }

    private record InventorySnapshot(
            List<ItemStack> items,
            List<ItemStack> offhand,
            List<ItemStack> armor
    ) {
        private static InventorySnapshot capture(ServerPlayer player) {
            return new InventorySnapshot(
                    copyStacks(player.getInventory().items),
                    copyStacks(player.getInventory().offhand),
                    copyStacks(player.getInventory().armor)
            );
        }

        private void restore(ServerPlayer player) {
            restoreStacks(player.getInventory().items, items);
            restoreStacks(player.getInventory().offhand, offhand);
            restoreStacks(player.getInventory().armor, armor);
            player.getInventory().setChanged();
        }

        private static List<ItemStack> copyStacks(NonNullList<ItemStack> stacks) {
            List<ItemStack> copiedStacks = new ArrayList<>(stacks.size());
            for (ItemStack stack : stacks) {
                copiedStacks.add(stack.copy());
            }
            return copiedStacks;
        }

        private static void restoreStacks(NonNullList<ItemStack> target, List<ItemStack> source) {
            for (int i = 0; i < target.size() && i < source.size(); i++) {
                target.set(i, source.get(i).copy());
            }
        }
    }
}
