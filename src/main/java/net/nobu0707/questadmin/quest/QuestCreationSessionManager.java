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

public final class QuestCreationSessionManager {
    private static final Map<UUID, QuestCreationSession> SESSIONS = new ConcurrentHashMap<>();

    private QuestCreationSessionManager() {
    }

    public static ActionResult start(ServerPlayer player) {
        if (!hasAdminPermission(player)) {
            return ActionResult.failure("QuestAdmin: この操作にはOP権限レベル2以上が必要です。");
        }

        if (hasSession(player.getUUID())) {
            return ActionResult.failure("QuestAdmin: 既にクエスト作成中です。cancel または /questadmin create cancel で終了してください。");
        }

        if (QuestEditSessionManager.hasSession(player.getUUID())) {
            return ActionResult.failure("QuestAdmin: クエスト編集中です。cancel または /questadmin edit cancel で終了してください。");
        }

        QuestCreationSession session = new QuestCreationSession();
        SESSIONS.put(player.getUUID(), session);
        player.closeContainer();
        player.sendSystemMessage(Component.literal("QuestAdmin: 新規クエスト作成を開始します。"));
        player.sendSystemMessage(Component.literal("QuestAdmin: " + session.getStep().getPrompt()));
        player.sendSystemMessage(Component.literal("QuestAdmin: cancel または /questadmin create cancel でキャンセルできます。").withStyle(ChatFormatting.GRAY));
        return ActionResult.success("QuestAdmin: チャット入力で作成を進めてください。");
    }

    public static ActionResult cancel(ServerPlayer player) {
        if (!hasAdminPermission(player)) {
            return ActionResult.failure("QuestAdmin: この操作にはOP権限レベル2以上が必要です。");
        }

        QuestCreationSession removedSession = SESSIONS.remove(player.getUUID());
        if (removedSession == null) {
            return ActionResult.failure("QuestAdmin: 進行中のクエスト作成はありません。");
        }

        return ActionResult.success("QuestAdmin: クエスト作成をキャンセルしました。");
    }

    public static boolean hasSession(UUID playerUuid) {
        return SESSIONS.containsKey(playerUuid);
    }

    public static void onServerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        QuestCreationSession session = SESSIONS.get(player.getUUID());
        if (session == null) {
            return;
        }

        event.setCanceled(true);

        if (!hasAdminPermission(player)) {
            SESSIONS.remove(player.getUUID());
            player.sendSystemMessage(Component.literal("QuestAdmin: 権限がないため、クエスト作成をキャンセルしました。").withStyle(ChatFormatting.RED));
            return;
        }

        QuestCreationSession.StepResult result = session.accept(event.getRawText(), QuestAdminMod.getQuestStorage());
        if (result.cancelled()) {
            SESSIONS.remove(player.getUUID());
            player.sendSystemMessage(Component.literal("QuestAdmin: クエスト作成をキャンセルしました。"));
            return;
        }

        if (!result.success()) {
            player.sendSystemMessage(Component.literal(result.message()).withStyle(ChatFormatting.RED));
            player.sendSystemMessage(Component.literal("QuestAdmin: " + session.getStep().getPrompt()));
            return;
        }

        if (!result.completed()) {
            player.sendSystemMessage(Component.literal("QuestAdmin: " + result.message()));
            return;
        }

        saveQuest(player, result.quest());
    }

    private static void saveQuest(ServerPlayer player, QuestDefinition quest) {
        QuestStorage storage = QuestAdminMod.getQuestStorage();
        if (storage.exists(quest.getId())) {
            SESSIONS.remove(player.getUUID());
            player.sendSystemMessage(Component.literal("QuestAdmin: 既に同じquestIdのクエストが存在します: " + quest.getId()).withStyle(ChatFormatting.RED));
            return;
        }

        ArrayList<QuestDefinition> updatedQuests = new ArrayList<>(storage.getQuests());
        updatedQuests.add(quest);
        QuestValidationResult validationResult = QuestValidator.validateQuestList(updatedQuests);
        if (!validationResult.isValid()) {
            player.sendSystemMessage(Component.literal(validationResult.firstMessage()).withStyle(ChatFormatting.RED));
            return;
        }

        try {
            storage.saveQuests(updatedQuests);
        } catch (IOException | RuntimeException exception) {
            player.sendSystemMessage(Component.literal("QuestAdmin: クエスト定義の保存に失敗しました: " + exception.getMessage()).withStyle(ChatFormatting.RED));
            return;
        }

        SESSIONS.remove(player.getUUID());
        player.sendSystemMessage(Component.literal("QuestAdmin: クエスト " + quest.getId() + " を作成しました。"));
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
