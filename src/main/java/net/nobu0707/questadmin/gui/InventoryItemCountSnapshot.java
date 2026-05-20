package net.nobu0707.questadmin.gui;

import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public final class InventoryItemCountSnapshot {
    private final Map<Item, Integer> counts;

    private InventoryItemCountSnapshot(Map<Item, Integer> counts) {
        this.counts = Map.copyOf(counts);
    }

    public static InventoryItemCountSnapshot capture(ServerPlayer player) {
        Map<Item, Integer> counts = new HashMap<>();
        addCounts(counts, player.getInventory().items);
        addCounts(counts, player.getInventory().offhand);
        return new InventoryItemCountSnapshot(counts);
    }

    public int count(Item item) {
        return counts.getOrDefault(item, 0);
    }

    private static void addCounts(Map<Item, Integer> counts, NonNullList<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }
    }
}
