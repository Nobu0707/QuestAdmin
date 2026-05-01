package net.nobu0707.questadmin.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.nobu0707.questadmin.quest.QuestDefinition;
import net.nobu0707.questadmin.quest.QuestRequirement;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class AdminQuestMenuItemFactory {
    private AdminQuestMenuItemFactory() {
    }

    public static ItemStack createListItem(QuestDefinition quest) {
        ItemStack stack = new ItemStack(getQuestIcon(quest));
        stack.setHoverName(Component.literal(quest.getTitle()).withStyle(quest.isEnabled() ? ChatFormatting.GREEN : ChatFormatting.GRAY));

        List<Component> lore = createAdminQuestLore(quest);
        lore.add(Component.empty());
        lore.add(Component.literal("左クリック: 有効/無効を切り替え").withStyle(ChatFormatting.YELLOW));
        lore.add(Component.literal("右クリック: 詳細を開く").withStyle(ChatFormatting.GRAY));
        lore.add(Component.literal("Shiftクリック: 削除確認").withStyle(ChatFormatting.RED));
        setLore(stack, lore);
        return stack;
    }

    public static ItemStack createDetailItem(QuestDefinition quest) {
        ItemStack stack = new ItemStack(resolveItem(quest.getRequirement().getItemId()).orElse(Items.BOOK));
        stack.setHoverName(Component.literal(quest.getTitle()).withStyle(ChatFormatting.AQUA));
        setLore(stack, createAdminQuestLore(quest));
        return stack;
    }

    public static ItemStack createToggleItem(QuestDefinition quest) {
        ItemStack stack = new ItemStack(quest.isEnabled() ? Items.GRAY_DYE : Items.LIME_DYE);
        String nextState = quest.isEnabled() ? "無効にする" : "有効にする";
        stack.setHoverName(Component.literal(nextState).withStyle(ChatFormatting.YELLOW));
        setLore(stack, List.of(
                Component.literal("現在: " + getEnabledLabel(quest)).withStyle(getEnabledColor(quest)),
                Component.literal("クリック: " + nextState).withStyle(ChatFormatting.GRAY)
        ));
        return stack;
    }

    public static ItemStack createDeleteItem(QuestDefinition quest) {
        ItemStack stack = new ItemStack(Items.BARRIER);
        stack.setHoverName(Component.literal("削除確認へ").withStyle(ChatFormatting.RED));
        setLore(stack, List.of(
                Component.literal("クエスト: " + quest.getTitle()).withStyle(ChatFormatting.GRAY),
                Component.literal("ID: " + quest.getId()).withStyle(ChatFormatting.DARK_GRAY),
                Component.empty(),
                Component.literal("クリックして確認画面を開く").withStyle(ChatFormatting.RED)
        ));
        return stack;
    }

    public static ItemStack createConfirmDeleteItem(QuestDefinition quest) {
        ItemStack stack = new ItemStack(Items.RED_CONCRETE);
        stack.setHoverName(Component.literal("削除する").withStyle(ChatFormatting.RED));
        setLore(stack, List.of(
                Component.literal("クエスト: " + quest.getTitle()).withStyle(ChatFormatting.GRAY),
                Component.literal("ID: " + quest.getId()).withStyle(ChatFormatting.DARK_GRAY),
                Component.empty(),
                Component.literal("クリックするとquests.jsonから削除します。").withStyle(ChatFormatting.RED)
        ));
        return stack;
    }

    public static ItemStack createCancelDeleteItem() {
        ItemStack stack = new ItemStack(Items.LIME_CONCRETE);
        stack.setHoverName(Component.literal("キャンセル").withStyle(ChatFormatting.GREEN));
        setLore(stack, List.of(Component.literal("クリック: 管理一覧へ戻る").withStyle(ChatFormatting.GRAY)));
        return stack;
    }

    public static ItemStack createCreateQuestItem() {
        ItemStack stack = new ItemStack(Items.WRITABLE_BOOK);
        stack.setHoverName(Component.literal("新規クエスト作成").withStyle(ChatFormatting.AQUA));
        setLore(stack, List.of(
                Component.literal("Phase 8で実装予定です。").withStyle(ChatFormatting.GRAY),
                Component.literal("クリック: 予定メッセージを表示").withStyle(ChatFormatting.YELLOW)
        ));
        return stack;
    }

    public static ItemStack createBackItem() {
        ItemStack stack = new ItemStack(Items.ARROW);
        stack.setHoverName(Component.literal("管理一覧へ戻る").withStyle(ChatFormatting.YELLOW));
        setLore(stack, List.of(Component.literal("クリック: 一覧を開く").withStyle(ChatFormatting.GRAY)));
        return stack;
    }

    public static ItemStack createEmptyListItem() {
        ItemStack stack = new ItemStack(Items.PAPER);
        stack.setHoverName(Component.literal("登録済みクエストはありません").withStyle(ChatFormatting.GRAY));
        setLore(stack, List.of(Component.literal("クエストが登録されるとここに表示されます。").withStyle(ChatFormatting.DARK_GRAY)));
        return stack;
    }

    public static ItemStack createMissingQuestItem(String questId) {
        ItemStack stack = new ItemStack(Items.BARRIER);
        stack.setHoverName(Component.literal("クエストが見つかりません").withStyle(ChatFormatting.RED));
        setLore(stack, List.of(Component.literal("ID: " + questId).withStyle(ChatFormatting.DARK_GRAY)));
        return stack;
    }

    public static ItemStack createNoPermissionItem() {
        ItemStack stack = new ItemStack(Items.BARRIER);
        stack.setHoverName(Component.literal("権限がありません").withStyle(ChatFormatting.RED));
        setLore(stack, List.of(Component.literal("OP権限レベル2以上が必要です。").withStyle(ChatFormatting.GRAY)));
        return stack;
    }

    public static ItemStack createFillerItem() {
        ItemStack stack = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        stack.setHoverName(Component.literal(" "));
        return stack;
    }

    private static List<Component> createAdminQuestLore(QuestDefinition quest) {
        QuestRequirement requirement = quest.getRequirement();
        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal(quest.getDescription()).withStyle(ChatFormatting.GRAY));
        lore.add(Component.empty());
        lore.add(Component.literal("ID: " + quest.getId()).withStyle(ChatFormatting.DARK_GRAY));
        lore.add(Component.literal("Type: " + quest.getType()).withStyle(ChatFormatting.GRAY));
        lore.add(Component.literal("状態: " + getEnabledLabel(quest)).withStyle(getEnabledColor(quest)));
        lore.add(Component.literal("必要: " + requirement.getItemId() + " x" + requirement.getAmount()).withStyle(ChatFormatting.GRAY));
        lore.add(Component.literal("報酬: " + quest.getReward().getMoney()).withStyle(ChatFormatting.GOLD));
        lore.add(Component.literal("繰り返し: " + quest.isRepeatable()).withStyle(ChatFormatting.GRAY));
        return lore;
    }

    private static Item getQuestIcon(QuestDefinition quest) {
        if (!quest.isEnabled()) {
            return Items.GRAY_DYE;
        }
        return resolveItem(quest.getRequirement().getItemId()).orElse(Items.BOOK);
    }

    private static String getEnabledLabel(QuestDefinition quest) {
        return quest.isEnabled() ? "有効" : "無効";
    }

    private static ChatFormatting getEnabledColor(QuestDefinition quest) {
        return quest.isEnabled() ? ChatFormatting.GREEN : ChatFormatting.GRAY;
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

    private static void setLore(ItemStack stack, List<Component> lore) {
        ListTag loreTag = new ListTag();
        for (Component line : lore) {
            loreTag.add(StringTag.valueOf(Component.Serializer.toJson(line)));
        }
        stack.getOrCreateTagElement("display").put("Lore", loreTag);
    }
}
