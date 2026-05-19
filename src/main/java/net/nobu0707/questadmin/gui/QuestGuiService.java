package net.nobu0707.questadmin.gui;

import net.minecraft.server.level.ServerPlayer;
import net.nobu0707.questadmin.QuestAdminMod;
import net.nobu0707.questadmin.quest.PlayerQuestState;
import net.nobu0707.questadmin.quest.QuestCompletionService;
import net.nobu0707.questadmin.quest.QuestDefinition;
import net.nobu0707.questadmin.quest.QuestRewardService;
import net.nobu0707.questadmin.quest.QuestStatus;

import java.util.List;

public final class QuestGuiService {
    public List<QuestDefinition> getVisibleQuests() {
        return QuestAdminMod.getQuestStorage().getQuests().stream()
                .filter(QuestDefinition::isEnabled)
                .toList();
    }

    public QuestStatus getStatus(ServerPlayer player, QuestDefinition quest) {
        return QuestAdminMod.getPlayerQuestStorage().getState(player.getUUID(), quest.getId())
                .map(PlayerQuestState::getStatus)
                .orElse(QuestStatus.NOT_STARTED);
    }

    public ActionResult activateQuest(ServerPlayer player, String questId) {
        QuestDefinition quest = findEnabledQuest(questId);
        if (quest == null) {
            return ActionResult.failure("QuestAdmin: 存在しない、または無効なクエストです: " + questId);
        }

        QuestStatus status = getStatus(player, quest);
        if (status == QuestStatus.CLAIMED) {
            return ActionResult.failure("QuestAdmin: このクエストの報酬は既に受け取り済みです。");
        }

        if (status == QuestStatus.CLAIMING) {
            return ActionResult.failure("QuestAdmin: 報酬受け取り処理中、または支払い結果の確認が必要です。管理者へ連絡してください。");
        }

        if (status == QuestStatus.COMPLETED) {
            QuestRewardService.ClaimResult result = createRewardService().claimReward(player, questId);
            return new ActionResult(result.success(), result.message(), result.secondaryMessage(), true);
        }

        QuestCompletionService.CompletionResult result = createCompletionService().completeItemDeliveryQuest(player, questId);
        if (!result.success()) {
            return ActionResult.failure(result.message());
        }

        return ActionResult.success(
                "QuestAdmin: クエスト「" + result.quest().getTitle() + "」を完了しました。",
                "QuestAdmin: 報酬は /quest claim " + result.quest().getId() + " で受け取れます。"
        );
    }

    private QuestDefinition findEnabledQuest(String questId) {
        return QuestAdminMod.getQuestStorage().getQuests().stream()
                .filter(QuestDefinition::isEnabled)
                .filter(quest -> quest.getId().equals(questId))
                .findFirst()
                .orElse(null);
    }

    private QuestCompletionService createCompletionService() {
        return new QuestCompletionService(
                QuestAdminMod.getQuestStorage(),
                QuestAdminMod.getPlayerQuestStorage()
        );
    }

    private QuestRewardService createRewardService() {
        return new QuestRewardService(
                QuestAdminMod.getQuestStorage(),
                QuestAdminMod.getPlayerQuestStorage(),
                QuestAdminMod.getEconomyService()
        );
    }

    public record ActionResult(boolean success, String message, String secondaryMessage, boolean refresh) {
        private static ActionResult success(String message, String secondaryMessage) {
            return new ActionResult(true, message, secondaryMessage, true);
        }

        private static ActionResult failure(String message) {
            return new ActionResult(false, message, "", true);
        }

        public boolean hasSecondaryMessage() {
            return !secondaryMessage.isBlank();
        }
    }
}
