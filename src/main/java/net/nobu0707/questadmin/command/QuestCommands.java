package net.nobu0707.questadmin.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.nobu0707.questadmin.QuestAdminMod;
import net.nobu0707.questadmin.gui.AdminQuestMenuProvider;
import net.nobu0707.questadmin.gui.QuestMenuProvider;
import net.nobu0707.questadmin.quest.PlayerQuestState;
import net.nobu0707.questadmin.quest.PlayerQuestStorage;
import net.nobu0707.questadmin.quest.QuestCreationSessionManager;
import net.nobu0707.questadmin.quest.QuestCompletionService;
import net.nobu0707.questadmin.quest.QuestDefinition;
import net.nobu0707.questadmin.quest.QuestEditSessionManager;
import net.nobu0707.questadmin.quest.QuestRequirement;
import net.nobu0707.questadmin.quest.QuestRewardService;
import net.nobu0707.questadmin.quest.QuestStatus;
import net.nobu0707.questadmin.quest.QuestStorage;
import net.minecraftforge.event.RegisterCommandsEvent;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class QuestCommands {
    private QuestCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("questadmin")
                .executes(context -> openAdminQuestGui(context.getSource()))
                .then(Commands.literal("reload")
                        .executes(context -> reloadQuests(context.getSource())))
                .then(Commands.literal("list")
                        .executes(context -> listAdminQuests(context.getSource())))
                .then(Commands.literal("create")
                        .then(Commands.literal("cancel")
                                .executes(context -> cancelQuestCreation(context.getSource()))))
                .then(Commands.literal("edit")
                        .then(Commands.literal("cancel")
                                .executes(context -> cancelQuestEdit(context.getSource())))
                        .then(Commands.argument("questId", StringArgumentType.word())
                                .executes(context -> startQuestEdit(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "questId")
                                ))))
                .then(Commands.literal("economy")
                        .then(Commands.literal("status")
                                .executes(context -> showEconomyStatus(context.getSource()))))
                .then(Commands.literal("progress")
                        .then(Commands.literal("mark")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("questId", StringArgumentType.word())
                                                .then(Commands.argument("status", StringArgumentType.word())
                                                        .executes(context -> markPlayerProgress(
                                                                context.getSource(),
                                                                EntityArgument.getPlayer(context, "player"),
                                                                StringArgumentType.getString(context, "questId"),
                                                                StringArgumentType.getString(context, "status")
                                                        ))))))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> listPlayerProgress(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "player")
                                )))));

        dispatcher.register(Commands.literal("quest")
                .executes(context -> openQuestGui(context.getSource()))
                .then(Commands.literal("list")
                        .executes(context -> listPlayerQuests(context.getSource())))
                .then(Commands.literal("complete")
                        .then(Commands.argument("questId", StringArgumentType.word())
                                .executes(context -> completeQuest(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "questId")
                                ))))
                .then(Commands.literal("claim")
                        .then(Commands.argument("questId", StringArgumentType.word())
                                .executes(context -> claimQuestReward(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "questId")
                                )))));
    }

    private static int reloadQuests(CommandSourceStack source) {
        if (!hasAdminPermission(source)) {
            return sendNoAdminPermission(source);
        }

        QuestStorage.LoadResult result = QuestAdminMod.getQuestStorage().reloadQuests();
        if (!result.success()) {
            source.sendFailure(Component.literal("QuestAdmin: クエスト定義の再読み込みに失敗しました: " + result.errorMessage()));
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal("QuestAdmin: クエスト定義を再読み込みしました。読み込み件数: " + result.quests().size()),
                true
        );
        return result.quests().size();
    }

    private static int listAdminQuests(CommandSourceStack source) {
        if (!hasAdminPermission(source)) {
            return sendNoAdminPermission(source);
        }

        List<QuestDefinition> quests = QuestAdminMod.getQuestStorage().getQuests();
        source.sendSuccess(() -> Component.literal("QuestAdmin クエスト一覧:"), false);

        if (quests.isEmpty()) {
            source.sendSuccess(() -> Component.literal("- 登録済みクエストはありません。"), false);
            return 0;
        }

        for (QuestDefinition quest : quests) {
            source.sendSuccess(() -> Component.literal(formatAdminQuest(quest)), false);
        }
        return quests.size();
    }

    private static int showEconomyStatus(CommandSourceStack source) {
        if (!hasAdminPermission(source)) {
            return sendNoAdminPermission(source);
        }

        List<String> statusLines = QuestAdminMod.getEconomyService().getStatusLines();
        for (String statusLine : statusLines) {
            source.sendSuccess(() -> Component.literal(statusLine), false);
        }
        return 1;
    }

    private static int openAdminQuestGui(CommandSourceStack source) throws CommandSyntaxException {
        if (!hasAdminPermission(source)) {
            return sendNoAdminPermission(source);
        }

        ServerPlayer player = source.getPlayerOrException();
        AdminQuestMenuProvider.openQuestList(player);
        return 1;
    }

    private static int cancelQuestCreation(CommandSourceStack source) throws CommandSyntaxException {
        if (!hasAdminPermission(source)) {
            return sendNoAdminPermission(source);
        }

        ServerPlayer player = source.getPlayerOrException();
        QuestCreationSessionManager.ActionResult result = QuestCreationSessionManager.cancel(player);
        if (!result.success()) {
            source.sendFailure(Component.literal(result.message()));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(result.message()), false);
        return 1;
    }

    private static int startQuestEdit(CommandSourceStack source, String questId) throws CommandSyntaxException {
        if (!hasAdminPermission(source)) {
            return sendNoAdminPermission(source);
        }

        ServerPlayer player = source.getPlayerOrException();
        QuestEditSessionManager.ActionResult result = QuestEditSessionManager.start(player, questId);
        if (!result.success()) {
            source.sendFailure(Component.literal(result.message()));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(result.message()), false);
        return 1;
    }

    private static int cancelQuestEdit(CommandSourceStack source) throws CommandSyntaxException {
        if (!hasAdminPermission(source)) {
            return sendNoAdminPermission(source);
        }

        ServerPlayer player = source.getPlayerOrException();
        QuestEditSessionManager.ActionResult result = QuestEditSessionManager.cancel(player);
        if (!result.success()) {
            source.sendFailure(Component.literal(result.message()));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(result.message()), false);
        return 1;
    }

    private static int listPlayerQuests(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        List<QuestDefinition> enabledQuests = QuestAdminMod.getQuestStorage().getQuests().stream()
                .filter(QuestDefinition::isEnabled)
                .toList();

        source.sendSuccess(() -> Component.literal("受注可能なクエスト:"), false);
        if (enabledQuests.isEmpty()) {
            source.sendSuccess(() -> Component.literal("- 現在受注可能なクエストはありません。"), false);
            return 0;
        }

        for (QuestDefinition quest : enabledQuests) {
            QuestRequirement requirement = quest.getRequirement();
            source.sendSuccess(() -> Component.literal("- " + quest.getTitle() + " [" + getQuestStatusLabel(player, quest) + "]"), false);
            source.sendSuccess(() -> Component.literal("  " + quest.getDescription()), false);
            source.sendSuccess(() -> Component.literal("  必要: " + requirement.getItemId() + " x" + requirement.getAmount()), false);
            source.sendSuccess(() -> Component.literal("  報酬: " + quest.getReward().getMoney()), false);
        }
        return enabledQuests.size();
    }

    private static int openQuestGui(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        QuestMenuProvider.openQuestList(player);
        return 1;
    }

    private static int completeQuest(CommandSourceStack source, String questId) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        QuestCompletionService.CompletionResult result = createCompletionService().completeItemDeliveryQuest(player, questId);
        if (!result.success()) {
            source.sendFailure(Component.literal(result.message()));
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal("QuestAdmin: クエスト「" + result.quest().getTitle() + "」を完了しました。"),
                false
        );
        source.sendSuccess(
                () -> Component.literal("QuestAdmin: 報酬は /quest claim " + result.quest().getId() + " で受け取れます。"),
                false
        );
        return 1;
    }

    private static int claimQuestReward(CommandSourceStack source, String questId) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        QuestRewardService.ClaimResult result = createRewardService().claimReward(player, questId);
        if (!result.success()) {
            source.sendFailure(Component.literal(result.message()));
            if (result.hasSecondaryMessage()) {
                source.sendFailure(Component.literal(result.secondaryMessage()));
            }
            return 0;
        }

        source.sendSuccess(() -> Component.literal(result.message()), false);
        return 1;
    }

    private static int listPlayerProgress(CommandSourceStack source, ServerPlayer player) {
        if (!hasAdminPermission(source)) {
            return sendNoAdminPermission(source);
        }

        UUID playerUuid = player.getUUID();
        Map<String, PlayerQuestState> states = QuestAdminMod.getPlayerQuestStorage().getStates(playerUuid);

        source.sendSuccess(() -> Component.literal("QuestAdmin: " + player.getGameProfile().getName() + " のクエスト状態:"), false);
        if (states.isEmpty()) {
            source.sendSuccess(() -> Component.literal("- 保存済みクエスト状態はありません。"), false);
            return 0;
        }

        for (PlayerQuestState state : states.values()) {
            source.sendSuccess(() -> Component.literal(formatPlayerProgress(state)), false);
        }
        return states.size();
    }

    private static int markPlayerProgress(CommandSourceStack source, ServerPlayer player, String questId, String statusName) {
        if (!hasAdminPermission(source)) {
            return sendNoAdminPermission(source);
        }

        QuestStatus status = parseStatus(statusName);
        if (status == null) {
            source.sendFailure(Component.literal("QuestAdmin: 不明なステータスです: " + statusName));
            return 0;
        }

        if (findQuest(questId) == null) {
            source.sendFailure(Component.literal("QuestAdmin: 存在しないクエストIDです: " + questId));
            return 0;
        }

        PlayerQuestStorage storage = QuestAdminMod.getPlayerQuestStorage();
        try {
            storage.markStatus(player.getUUID(), questId, status);
        } catch (IllegalStateException exception) {
            source.sendFailure(Component.literal("QuestAdmin: クエスト状態を変更できません: " + exception.getMessage()));
            return 0;
        } catch (IOException exception) {
            source.sendFailure(Component.literal("QuestAdmin: クエスト状態の保存に失敗しました: " + exception.getMessage()));
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal("QuestAdmin: " + player.getGameProfile().getName() + " の " + questId + " を " + status + " として保存しました。"),
                true
        );
        return 1;
    }

    private static boolean hasAdminPermission(CommandSourceStack source) {
        return source.hasPermission(2);
    }

    private static int sendNoAdminPermission(CommandSourceStack source) {
        source.sendFailure(Component.literal("QuestAdmin: この操作にはOP権限レベル2以上が必要です。"));
        return 0;
    }

    private static String formatAdminQuest(QuestDefinition quest) {
        return "- " + quest.getId()
                + " | " + quest.getTitle()
                + " | " + quest.getType()
                + " | enabled=" + quest.isEnabled()
                + " | reward=" + quest.getReward().getMoney();
    }

    private static String formatPlayerProgress(PlayerQuestState state) {
        return "- " + state.getQuestId()
                + " | status=" + state.getStatus()
                + " | completedAt=" + state.getCompletedAt()
                + " | claimedAt=" + state.getClaimedAt();
    }

    private static QuestDefinition findQuest(String questId) {
        return QuestAdminMod.getQuestStorage().getQuests().stream()
                .filter(quest -> quest.getId().equals(questId))
                .findFirst()
                .orElse(null);
    }

    private static String getQuestStatusLabel(ServerPlayer player, QuestDefinition quest) {
        return QuestAdminMod.getPlayerQuestStorage().getState(player.getUUID(), quest.getId())
                .map(PlayerQuestState::getStatus)
                .map(status -> switch (status) {
                    case CLAIMED -> "報酬受取済み";
                    case COMPLETED -> "完了済み";
                    case NOT_STARTED -> "未完了";
                })
                .orElse("未完了");
    }

    private static QuestCompletionService createCompletionService() {
        return new QuestCompletionService(
                QuestAdminMod.getQuestStorage(),
                QuestAdminMod.getPlayerQuestStorage()
        );
    }

    private static QuestRewardService createRewardService() {
        return new QuestRewardService(
                QuestAdminMod.getQuestStorage(),
                QuestAdminMod.getPlayerQuestStorage(),
                QuestAdminMod.getEconomyService()
        );
    }

    private static QuestStatus parseStatus(String statusName) {
        try {
            return QuestStatus.valueOf(statusName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
