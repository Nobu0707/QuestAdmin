package net.nobu0707.questadmin.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;

public final class AdminQuestMenuProvider {
    private AdminQuestMenuProvider() {
    }

    public static void openQuestList(ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, ignored) -> new AdminQuestMenu(containerId, inventory, player),
                Component.literal("QuestAdmin 管理")
        ));
    }

    public static void openQuestDetail(ServerPlayer player, String questId) {
        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, ignored) -> new AdminQuestDetailMenu(containerId, inventory, player, questId),
                Component.literal("QuestAdmin 管理詳細")
        ));
    }

    public static void openDeleteConfirm(ServerPlayer player, String questId) {
        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, ignored) -> new QuestDeleteConfirmMenu(containerId, inventory, player, questId),
                Component.literal("QuestAdmin 削除確認")
        ));
    }
}
