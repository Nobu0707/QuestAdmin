package net.nobu0707.questadmin.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;

public final class QuestMenuProvider {
    private QuestMenuProvider() {
    }

    public static void openQuestList(ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, ignored) -> new QuestMenu(containerId, inventory, player),
                Component.literal("QuestAdmin クエスト")
        ));
    }

    public static void openQuestDetail(ServerPlayer player, String questId) {
        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, ignored) -> new QuestDetailMenu(containerId, inventory, player, questId),
                Component.literal("QuestAdmin 詳細")
        ));
    }
}
