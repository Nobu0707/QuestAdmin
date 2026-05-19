package net.nobu0707.questadmin.quest;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.nobu0707.questadmin.economy.EconomyService;
import org.slf4j.Logger;

import java.io.IOException;

public final class QuestRewardService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String CLAIMING_MESSAGE = "QuestAdmin: 報酬受け取り処理中、または支払い結果の確認が必要です。管理者へ連絡してください。";

    private final QuestStorage questStorage;
    private final PlayerQuestStorage playerQuestStorage;
    private final EconomyService economyService;

    public QuestRewardService(QuestStorage questStorage, PlayerQuestStorage playerQuestStorage, EconomyService economyService) {
        this.questStorage = questStorage;
        this.playerQuestStorage = playerQuestStorage;
        this.economyService = economyService;
    }

    public ClaimResult claimReward(ServerPlayer player, String questId) {
        try {
            synchronized (playerQuestStorage) {
                return claimRewardLocked(player, questId);
            }
        } catch (RuntimeException | LinkageError exception) {
            LOGGER.error(
                    "Unexpected error while claiming quest reward. playerUuid={}, questId={}",
                    player.getUUID(),
                    questId,
                    exception
            );
            return ClaimResult.failure("QuestAdmin: 報酬受け取り処理中にエラーが発生しました。管理者へ連絡してください。");
        }
    }

    private ClaimResult claimRewardLocked(ServerPlayer player, String questId) {
        QuestDefinition quest = findQuestById(questId);
        if (quest == null) {
            return ClaimResult.failure("QuestAdmin: 存在しないクエストIDです: " + questId);
        }

        if (!quest.isEnabled()) {
            return ClaimResult.failure("QuestAdmin: このクエストは現在無効です。");
        }

        PlayerQuestState state = playerQuestStorage.getState(player.getUUID(), questId).orElse(null);
        if (state != null && state.getStatus() == QuestStatus.CLAIMED) {
            return ClaimResult.failure("QuestAdmin: このクエストの報酬は既に受け取り済みです。");
        }

        if (state != null && state.getStatus() == QuestStatus.CLAIMING) {
            return ClaimResult.failure(CLAIMING_MESSAGE);
        }

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
        if (amount < 0) {
            return ClaimResult.failure("QuestAdmin: 報酬金額が不正です。");
        }

        try {
            playerQuestStorage.markClaiming(player.getUUID(), questId);
        } catch (IOException | IllegalStateException exception) {
            LOGGER.error(
                    "Failed to save CLAIMING state before reward deposit. playerUuid={}, questId={}",
                    player.getUUID(),
                    questId,
                    exception
            );
            return ClaimResult.failure(
                    "QuestAdmin: 報酬受け取り準備の保存に失敗しました。報酬は支払われていません。",
                    "QuestAdmin: しばらくしてから再実行するか、管理者へ連絡してください。"
            );
        }

        boolean deposited;
        try {
            deposited = amount == 0 || economyService.deposit(player.getUUID(), amount);
        } catch (RuntimeException | LinkageError exception) {
            LOGGER.error(
                    "Lightman's Currency deposit threw an exception. playerUuid={}, questId={}, amount={}",
                    player.getUUID(),
                    questId,
                    amount,
                    exception
            );
            restoreCompletedAfterFailedDeposit(player, questId);
            return ClaimResult.failure(
                    "QuestAdmin: 報酬の支払い中にエラーが発生しました。",
                    "QuestAdmin: クエスト状態は可能な限り完了済みに戻しました。"
            );
        }

        if (!deposited) {
            restoreCompletedAfterFailedDeposit(player, questId);
            return ClaimResult.failure(
                    "QuestAdmin: 報酬の支払いに失敗しました。",
                    "QuestAdmin: クエスト状態は可能な限り完了済みに戻しました。"
            );
        }

        try {
            playerQuestStorage.markClaimed(player.getUUID(), questId);
        } catch (IOException | IllegalStateException exception) {
            LOGGER.error(
                    "CRITICAL: Lightman's Currency deposit succeeded but CLAIMED state save failed. "
                            + "Do not retry payment until an admin verifies the player balance. playerUuid={}, questId={}, amount={}",
                    player.getUUID(),
                    questId,
                    amount,
                    exception
            );
            return ClaimResult.failure(
                    "QuestAdmin: 報酬支払い後の状態保存に失敗しました。管理者へ連絡してください。",
                    "QuestAdmin: 二重支払い防止のため、このクエストは確認が必要な状態として扱われます。"
            );
        }

        return ClaimResult.success("QuestAdmin: 報酬 " + amount + " " + economyService.getCurrencyName() + " を受け取りました。");
    }

    private void restoreCompletedAfterFailedDeposit(ServerPlayer player, String questId) {
        try {
            playerQuestStorage.markCompleted(player.getUUID(), questId);
        } catch (IOException | IllegalStateException exception) {
            LOGGER.error(
                    "Failed to restore quest state to COMPLETED after an unsuccessful reward deposit. "
                            + "The quest may remain CLAIMING and require admin review. playerUuid={}, questId={}",
                    player.getUUID(),
                    questId,
                    exception
            );
        }
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
