package net.nobu0707.questadmin.gui;

import net.minecraft.server.level.ServerPlayer;
import net.nobu0707.questadmin.QuestAdminMod;
import net.nobu0707.questadmin.quest.QuestDefinition;
import net.nobu0707.questadmin.quest.QuestStorage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class AdminQuestGuiService {
    public List<QuestDefinition> getAllQuests() {
        return QuestAdminMod.getQuestStorage().getQuests();
    }

    public Optional<QuestDefinition> findQuest(String questId) {
        return getAllQuests().stream()
                .filter(quest -> quest.getId().equals(questId))
                .findFirst();
    }

    public ActionResult toggleEnabled(ServerPlayer player, String questId) {
        if (!hasAdminPermission(player)) {
            return ActionResult.failure("QuestAdmin: この操作にはOP権限レベル2以上が必要です。");
        }

        QuestStorage storage = QuestAdminMod.getQuestStorage();
        List<QuestDefinition> updatedQuests = new ArrayList<>(storage.getQuests().size());
        QuestDefinition updatedQuest = null;

        for (QuestDefinition quest : storage.getQuests()) {
            if (quest.getId().equals(questId)) {
                updatedQuest = copyWithEnabled(quest, !quest.isEnabled());
                updatedQuests.add(updatedQuest);
            } else {
                updatedQuests.add(quest);
            }
        }

        if (updatedQuest == null) {
            return ActionResult.failure("QuestAdmin: 存在しないクエストIDです: " + questId);
        }

        try {
            storage.saveQuests(updatedQuests);
        } catch (IOException | RuntimeException exception) {
            return ActionResult.failure("QuestAdmin: クエスト定義の保存に失敗しました: " + exception.getMessage());
        }

        String stateLabel = updatedQuest.isEnabled() ? "有効" : "無効";
        return ActionResult.success("QuestAdmin: クエスト「" + updatedQuest.getTitle() + "」を" + stateLabel + "にしました。");
    }

    public ActionResult deleteQuest(ServerPlayer player, String questId) {
        if (!hasAdminPermission(player)) {
            return ActionResult.failure("QuestAdmin: この操作にはOP権限レベル2以上が必要です。");
        }

        QuestStorage storage = QuestAdminMod.getQuestStorage();
        List<QuestDefinition> updatedQuests = new ArrayList<>();
        QuestDefinition deletedQuest = null;

        for (QuestDefinition quest : storage.getQuests()) {
            if (quest.getId().equals(questId)) {
                deletedQuest = quest;
            } else {
                updatedQuests.add(quest);
            }
        }

        if (deletedQuest == null) {
            return ActionResult.failure("QuestAdmin: 存在しないクエストIDです: " + questId);
        }

        try {
            storage.saveQuests(updatedQuests);
        } catch (IOException | RuntimeException exception) {
            return ActionResult.failure("QuestAdmin: クエスト定義の保存に失敗しました: " + exception.getMessage());
        }

        return ActionResult.success("QuestAdmin: クエスト「" + deletedQuest.getTitle() + "」を削除しました。");
    }

    public ActionResult showCreateNotImplemented(ServerPlayer player) {
        if (!hasAdminPermission(player)) {
            return ActionResult.failure("QuestAdmin: この操作にはOP権限レベル2以上が必要です。");
        }
        return ActionResult.success("QuestAdmin: クエスト作成GUIは Phase 8 で実装予定です。");
    }

    public boolean hasAdminPermission(ServerPlayer player) {
        return player.hasPermissions(2);
    }

    private static QuestDefinition copyWithEnabled(QuestDefinition quest, boolean enabled) {
        return new QuestDefinition(
                quest.getId(),
                quest.getTitle(),
                quest.getDescription(),
                quest.getType(),
                quest.getRequirement(),
                quest.getReward(),
                quest.isRepeatable(),
                enabled,
                quest.getCreatedAt(),
                System.currentTimeMillis()
        );
    }

    public record ActionResult(boolean success, String message) {
        private static ActionResult success(String message) {
            return new ActionResult(true, message);
        }

        private static ActionResult failure(String message) {
            return new ActionResult(false, message);
        }
    }
}
