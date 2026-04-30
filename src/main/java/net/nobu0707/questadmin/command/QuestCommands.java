package net.nobu0707.questadmin.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.nobu0707.questadmin.QuestAdminMod;
import net.nobu0707.questadmin.quest.PlayerQuestState;
import net.nobu0707.questadmin.quest.PlayerQuestStorage;
import net.nobu0707.questadmin.quest.QuestDefinition;
import net.nobu0707.questadmin.quest.QuestRequirement;
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
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("reload")
                        .executes(context -> reloadQuests(context.getSource())))
                .then(Commands.literal("list")
                        .executes(context -> listAdminQuests(context.getSource())))
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
                .then(Commands.literal("list")
                        .executes(context -> listPlayerQuests(context.getSource()))));
    }

    private static int reloadQuests(CommandSourceStack source) {
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

    private static int listPlayerQuests(CommandSourceStack source) {
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
            source.sendSuccess(() -> Component.literal("- " + quest.getTitle()), false);
            source.sendSuccess(() -> Component.literal("  " + quest.getDescription()), false);
            source.sendSuccess(() -> Component.literal("  必要: " + requirement.getItemId() + " x" + requirement.getAmount()), false);
            source.sendSuccess(() -> Component.literal("  報酬: " + quest.getReward().getMoney()), false);
        }
        return enabledQuests.size();
    }

    private static int listPlayerProgress(CommandSourceStack source, ServerPlayer player) {
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

    private static QuestStatus parseStatus(String statusName) {
        try {
            return QuestStatus.valueOf(statusName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
