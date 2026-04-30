package net.nobu0707.questadmin.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.nobu0707.questadmin.QuestAdminMod;
import net.nobu0707.questadmin.quest.QuestDefinition;
import net.nobu0707.questadmin.quest.QuestRequirement;
import net.nobu0707.questadmin.quest.QuestStorage;
import net.minecraftforge.event.RegisterCommandsEvent;

import java.util.List;

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
                        .executes(context -> listAdminQuests(context.getSource()))));

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

    private static String formatAdminQuest(QuestDefinition quest) {
        return "- " + quest.getId()
                + " | " + quest.getTitle()
                + " | " + quest.getType()
                + " | enabled=" + quest.isEnabled()
                + " | reward=" + quest.getReward().getMoney();
    }
}
