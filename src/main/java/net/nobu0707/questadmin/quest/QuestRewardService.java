package net.nobu0707.questadmin.quest;

import net.minecraft.server.level.ServerPlayer;
import net.nobu0707.questadmin.economy.EconomyService;

import java.io.IOException;

public final class QuestRewardService {
    private final QuestStorage questStorage;
    private final PlayerQuestStorage playerQuestStorage;
    private final EconomyService economyService;

    public QuestRewardService(QuestStorage questStorage, PlayerQuestStorage playerQuestStorage, EconomyService economyService) {
        this.questStorage = questStorage;
        this.playerQuestStorage = playerQuestStorage;
        this.economyService = economyService;
    }

    public ClaimResult claimReward(ServerPlayer player, String questId) {
        QuestDefinition quest = findQuestById(questId);
        if (quest == null) {
            return ClaimResult.failure("QuestAdmin: 存在しないクエストIDです: " + questId);
        }

        if (!quest.isEnabled()) {
            return ClaimResult.failure("QuestAdmin: このクエストは現在無効です。");
        }

        if (playerQuestStorage.hasClaimed(player.getUUID(), questId)) {
            return ClaimResult.failure("QuestAdmin: このクエストの報酬は既に受け取り済みです。");
        }

        PlayerQuestState state = playerQuestStorage.getState(player.getUUID(), questId).orElse(null);
        if (state == null || state.getStatus() != QuestStatus.COMPLETED) {
            return ClaimResult.failure("QuestAdmin: このクエストはまだ完了していません。");
        }

        if (!economyService.isAvailable()) {
            return ClaimResult.failure(
                    "QuestAdmin: 経済MODが利用できないため、報酬を支払えませんでした。",
                    "QuestAdmin: クエスト状態は完了済みのまま保持されます。"
            );
        }

        long amount = quest.getReward().getMoney();
        if (amount <= 0) {
            return ClaimResult.failure("QuestAdmin: 報酬金額が不正です。");
        }

        boolean deposited = economyService.deposit(player.getUUID(), amount);
        if (!deposited) {
            return ClaimResult.failure(
                    "QuestAdmin: 報酬の支払いに失敗しました。",
                    "QuestAdmin: クエスト状態は完了済みのまま保持されます。"
            );
        }

        try {
            playerQuestStorage.markClaimed(player.getUUID(), questId);
        } catch (IOException | IllegalStateException exception) {
            return ClaimResult.failure("QuestAdmin: 報酬支払い後の状態保存に失敗しました。管理者に連絡してください: " + exception.getMessage());
        }

        return ClaimResult.success("QuestAdmin: 報酬 " + amount + " " + economyService.getCurrencyName() + " を受け取りました。");
    }

    private QuestDefinition findQuestById(String questId) {
        return questStorage.getQuests().stream()
                .filter(quest -> quest.getId().equals(questId))
                .findFirst()
                .orElse(null);
    }

    public record ClaimResult(boolean success, String message, String secondaryMessage) {
        private static ClaimResult success(String message) {
            return new ClaimResult(true, message, "");
        }

        private static ClaimResult failure(String message) {
            return new ClaimResult(false, message, "");
        }

        private static ClaimResult failure(String message, String secondaryMessage) {
            return new ClaimResult(false, message, secondaryMessage);
        }

        public boolean hasSecondaryMessage() {
            return !secondaryMessage.isBlank();
        }
    }
}
