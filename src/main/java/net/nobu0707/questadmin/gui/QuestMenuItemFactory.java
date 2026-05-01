package net.nobu0707.questadmin.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.nobu0707.questadmin.quest.QuestDefinition;
import net.nobu0707.questadmin.quest.QuestRequirement;
import net.nobu0707.questadmin.quest.QuestStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class QuestMenuItemFactory {
    private QuestMenuItemFactory() {
    }

    public static ItemStack createListItem(ServerPlayer player, QuestDefinition quest, QuestStatus status) {
        ItemStack stack = new ItemStack(getListIcon(quest, status));
        stack.setHoverName(Component.literal(quest.getTitle()).withStyle(getStatusColor(status)));

        List<Component> lore = createQuestLore(player, quest, status);
        lore.add(Component.empty());
        lore.add(Component.literal(getPrimaryActionText(status)).withStyle(ChatFormatting.YELLOW));
        lore.add(Component.literal("右クリック: 詳細を開く").withStyle(ChatFormatting.GRAY));
        setLore(stack, lore);
        return stack;
    }

    public static ItemStack createDetailItem(ServerPlayer player, QuestDefinition quest, QuestStatus status) {
        ItemStack stack = new ItemStack(resolveItem(quest.getRequirement().getItemId()).orElse(Items.BOOK));
        stack.setHoverName(Component.literal(quest.getTitle()).withStyle(ChatFormatting.AQUA));

        List<Component> lore = createQuestLore(player, quest, status);
        lore.add(Component.empty());
        lore.add(Component.literal("中央のボタンで操作できます。").withStyle(ChatFormatting.GRAY));
        setLore(stack, lore);
        return stack;
    }

    public static ItemStack createActionItem(QuestDefinition quest, QuestStatus status) {
        Item item = switch (status) {
            case NOT_STARTED -> Items.WRITABLE_BOOK;
            case COMPLETED -> Items.EMERALD;
            case CLAIMED -> Items.BARRIER;
        };

        ItemStack stack = new ItemStack(item);
        stack.setHoverName(Component.literal(getActionTitle(status)).withStyle(getStatusColor(status)));
        setLore(stack, List.of(
                Component.literal("クエスト: " + quest.getTitle()).withStyle(ChatFormatting.GRAY),
                Component.literal("ID: " + quest.getId()).withStyle(ChatFormatting.DARK_GRAY),
                Component.empty(),
                Component.literal(getPrimaryActionText(status)).withStyle(ChatFormatting.YELLOW)
        ));
        return stack;
    }

    public static ItemStack createBackItem() {
        ItemStack stack = new ItemStack(Items.ARROW);
        stack.setHoverName(Component.literal("クエスト一覧へ戻る").withStyle(ChatFormatting.YELLOW));
        setLore(stack, List.of(Component.literal("クリック: 一覧を開く").withStyle(ChatFormatting.GRAY)));
        return stack;
    }

    public static ItemStack createEmptyListItem() {
        ItemStack stack = new ItemStack(Items.PAPER);
        stack.setHoverName(Component.literal("受注可能なクエストはありません").withStyle(ChatFormatting.GRAY));
        setLore(stack, List.of(Component.literal("有効なクエストが追加されるとここに表示されます。").withStyle(ChatFormatting.DARK_GRAY)));
        return stack;
    }

    public static ItemStack createMissingQuestItem(String questId) {
        ItemStack stack = new ItemStack(Items.BARRIER);
        stack.setHoverName(Component.literal("クエストが見つかりません").withStyle(ChatFormatting.RED));
        setLore(stack, List.of(Component.literal("ID: " + questId).withStyle(ChatFormatting.DARK_GRAY)));
        return stack;
    }

    public static ItemStack createFillerItem() {
        ItemStack stack = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        stack.setHoverName(Component.literal(" "));
        return stack;
    }

    private static List<Component> createQuestLore(ServerPlayer player, QuestDefinition quest, QuestStatus status) {
        QuestRequirement requirement = quest.getRequirement();
        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal(quest.getDescription()).withStyle(ChatFormatting.GRAY));
        lore.add(Component.empty());
        lore.add(Component.literal("状態: " + getStatusLabel(status)).withStyle(getStatusColor(status)));
        lore.add(Component.literal("必要: " + requirement.getItemId() + " x" + requirement.getAmount()).withStyle(ChatFormatting.GRAY));
        lore.add(Component.literal("所持: " + countRequirement(player, quest) + " / " + requirement.getAmount()).withStyle(ChatFormatting.GRAY));
        lore.add(Component.literal("報酬: " + quest.getReward().getMoney()).withStyle(ChatFormatting.GOLD));
        lore.add(Component.literal("ID: " + quest.getId()).withStyle(ChatFormatting.DARK_GRAY));
        return lore;
    }

    private static Item getListIcon(QuestDefinition quest, QuestStatus status) {
        return switch (status) {
            case NOT_STARTED -> resolveItem(quest.getRequirement().getItemId()).orElse(Items.BOOK);
            case COMPLETED -> Items.EMERALD;
            case CLAIMED -> Items.PAPER;
        };
    }

    private static int countRequirement(ServerPlayer player, QuestDefinition quest) {
        Optional<Item> item = resolveItem(quest.getRequirement().getItemId());
        return item.map(value -> new net.nobu0707.questadmin.quest.QuestCompletionService(
                net.nobu0707.questadmin.QuestAdminMod.getQuestStorage(),
                net.nobu0707.questadmin.QuestAdminMod.getPlayerQuestStorage()
        ).countItem(player, value)).orElse(0);
    }

    private static Optional<Item> resolveItem(String itemId) {
        ResourceLocation resourceLocation = ResourceLocation.tryParse(itemId);
        if (resourceLocation == null) {
            return Optional.empty();
        }
        Optional<Item> item = BuiltInRegistries.ITEM.getOptional(resourceLocation);
        if (item.isEmpty() || item.get() == Items.AIR) {
            return Optional.empty();
        }
        return item;
    }

    private static String getStatusLabel(QuestStatus status) {
        return switch (status) {
            case NOT_STARTED -> "未完了";
            case COMPLETED -> "完了済み";
            case CLAIMED -> "報酬受取済み";
        };
    }

    private static String getPrimaryActionText(QuestStatus status) {
        return switch (status) {
            case NOT_STARTED -> "左クリック: クエスト完了を試行";
            case COMPLETED -> "左クリック: 報酬を受け取る";
            case CLAIMED -> "左クリック: 受取済みを確認";
        };
    }

    private static String getActionTitle(QuestStatus status) {
        return switch (status) {
            case NOT_STARTED -> "クエスト完了を試行";
            case COMPLETED -> "報酬を受け取る";
            case CLAIMED -> "報酬受取済み";
        };
    }

    private static ChatFormatting getStatusColor(QuestStatus status) {
        return switch (status) {
            case NOT_STARTED -> ChatFormatting.WHITE;
            case COMPLETED -> ChatFormatting.GREEN;
            case CLAIMED -> ChatFormatting.GRAY;
        };
    }

    private static void setLore(ItemStack stack, List<Component> lore) {
        ListTag loreTag = new ListTag();
        for (Component line : lore) {
            loreTag.add(StringTag.valueOf(Component.Serializer.toJson(line)));
        }
        stack.getOrCreateTagElement("display").put("Lore", loreTag);
    }
}
