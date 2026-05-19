package net.nobu0707.questadmin.quest;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.nobu0707.questadmin.QuestAdminMod;
import net.nobu0707.questadmin.gui.AdminQuestMenuProvider;
import net.minecraftforge.event.ServerChatEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class QuestEditSessionManager {
    private static final Map<UUID, QuestEditSession> SESSIONS = new ConcurrentHashMap<>();

    private QuestEditSessionManager() {
    }

    public static ActionResult start(ServerPlayer player, String questId) {
        if (!hasAdminPermission(player)) {
            return ActionResult.failure("QuestAdmin: この操作にはOP権限レベル2以上が必要です。");
        }

        if (hasSession(player.getUUID())) {
            return ActionResult.failure("QuestAdmin: 既にクエスト編集中です。cancel または /questadmin edit cancel で終了してください。");
        }

        if (QuestCreationSessionManager.hasSession(player.getUUID())) {
            return ActionResult.failure("QuestAdmin: クエスト作成中です。cancel または /questadmin create cancel で終了してください。");
        }

        QuestDefinition quest = QuestAdminMod.getQuestStorage().getQuests().stream()
                .filter(value -> value.getId().equals(questId))
                .findFirst()
                .orElse(null);
        if (quest == null) {
            return ActionResult.failure("QuestAdmin: クエストが見つかりません: " + questId);
        }

        if (quest.getType() != QuestType.ITEM_DELIVERY) {
            return ActionResult.failure("QuestAdmin: 編集できるクエスト種別は ITEM_DELIVERY のみです。");
        }

        QuestEditSession session = new QuestEditSession(quest);
        SESSIONS.put(player.getUUID(), session);
        player.closeContainer();
        player.sendSystemMessage(Component.literal("QuestAdmin: クエスト編集を開始します: " + questId));
        player.sendSystemMessage(Component.literal("QuestAdmin: " + session.getPrompt()));
        player.sendSystemMessage(Component.literal("QuestAdmin: 変更しない場合は - を入力してください。").withStyle(ChatFormatting.GRAY));
        player.sendSystemMessage(Component.literal("QuestAdmin: cancel または /questadmin edit cancel でキャンセルできます。").withStyle(ChatFormatting.GRAY));
        return ActionResult.success("QuestAdmin: チャット入力で編集を進めてください。");
    }

    public static ActionResult cancel(ServerPlayer player) {
        if (!hasAdminPermission(player)) {
            return ActionResult.failure("QuestAdmin: この操作にはOP権限レベル2以上が必要です。");
        }

        QuestEditSession removedSession = SESSIONS.remove(player.getUUID());
        if (removedSession == null) {
            return ActionResult.failure("QuestAdmin: 進行中のクエスト編集はありません。");
        }

        return ActionResult.success("QuestAdmin: クエスト編集をキャンセルしました。");
    }

    public static boolean hasSession(UUID playerUuid) {
        return SESSIONS.containsKey(playerUuid);
    }

    public static void onServerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        QuestEditSession session = SESSIONS.get(player.getUUID());
        if (session == null) {
            return;
        }

        event.setCanceled(true);

        if (!hasAdminPermission(player)) {
            SESSIONS.remove(player.getUUID());
            player.sendSystemMessage(Component.literal("QuestAdmin: 権限がないため、クエスト編集をキャンセルしました。").withStyle(ChatFormatting.RED));
            return;
        }

        QuestEditSession.StepResult result = session.accept(event.getRawText());
        if (result.cancelled()) {
            SESSIONS.remove(player.getUUID());
            player.sendSystemMessage(Component.literal("QuestAdmin: クエスト編集をキャンセルしました。"));
            return;
        }

        if (!result.success()) {
            player.sendSystemMessage(Component.literal(result.message()).withStyle(ChatFormatting.RED));
            player.sendSystemMessage(Component.literal("QuestAdmin: " + session.getPrompt()));
            return;
        }

        if (!result.completed()) {
            player.sendSystemMessage(Component.literal(result.message()));
            if (session.getStep() == QuestEditStep.CONFIRM) {
                player.sendSystemMessage(Component.literal("QuestAdmin: " + session.getPrompt()));
            }
            return;
        }

        saveQuest(player, result.quest());
    }

    private static void saveQuest(ServerPlayer player, QuestDefinition updatedQuest) {
        QuestStorage storage = QuestAdminMod.getQuestStorage();
        ArrayList<QuestDefinition> updatedQuests = new ArrayList<>(storage.getQuests().size());
        boolean replaced = false;

        for (QuestDefinition quest : storage.getQuests()) {
            if (quest.getId().equals(updatedQuest.getId())) {
                updatedQuests.add(updatedQuest);
                replaced = true;
            } else {
                updatedQuests.add(quest);
            }
        }

        if (!replaced) {
            SESSIONS.remove(player.getUUID());
            player.sendSystemMessage(Component.literal("QuestAdmin: クエストが見つかりません: " + updatedQuest.getId()).withStyle(ChatFormatting.RED));
            return;
        }

        QuestValidationResult validationResult = QuestValidator.validateQuestList(updatedQuests);
        if (!validationResult.isValid()) {
            player.sendSystemMessage(Component.literal(validationResult.firstMessage()).withStyle(ChatFormatting.RED));
            return;
        }

        try {
            storage.saveQuests(updatedQuests);
        } catch (IOException | RuntimeException exception) {
            player.sendSystemMessage(Component.literal("QuestAdmin: 保存に失敗しました。ログを確認してください。詳細: " + exception.getMessage()).withStyle(ChatFormatting.RED));
            return;
        }

        SESSIONS.remove(player.getUUID());
        player.sendSystemMessage(Component.literal("QuestAdmin: クエスト " + updatedQuest.getId() + " を更新しました。"));
        AdminQuestMenuProvider.openQuestList(player);
    }

    private static boolean hasAdminPermission(ServerPlayer player) {
        return player.hasPermissions(2);
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
