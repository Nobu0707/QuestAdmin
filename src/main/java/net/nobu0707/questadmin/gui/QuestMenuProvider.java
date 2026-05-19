package net.nobu0707.questadmin.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;

public final class QuestMenuProvider {
    private QuestMenuProvider() {
    }

    public static void openQuestList(ServerPlayer player) {
        openQuestList(player, 0);
    }

    public static void openQuestList(ServerPlayer player, int page) {
        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, ignored) -> new QuestMenu(containerId, inventory, player, page),
                Component.literal("QuestAdmin クエスト")
        ));
    }

    public static void openQuestDetail(ServerPlayer player, String questId) {
        openQuestDetail(player, questId, 0);
    }

    public static void openQuestDetail(ServerPlayer player, String questId, int returnPage) {
        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, ignored) -> new QuestDetailMenu(containerId, inventory, player, questId, returnPage),
                Component.literal("QuestAdmin 詳細")
        ));
    }
}
