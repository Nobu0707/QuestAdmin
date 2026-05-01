package net.nobu0707.questadmin.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.nobu0707.questadmin.quest.QuestDefinition;

public final class AdminQuestDetailMenu extends ChestMenu {
    private static final int ROWS = 3;
    private static final int MENU_SIZE = ROWS * 9;
    private static final int DETAIL_SLOT = 10;
    private static final int TOGGLE_SLOT = 12;
    private static final int DELETE_SLOT = 14;
    private static final int BACK_SLOT = 16;

    private final ServerPlayer player;
    private final String questId;
    private final SimpleContainer questContainer;
    private final AdminQuestGuiService service = new AdminQuestGuiService();

    public AdminQuestDetailMenu(int containerId, Inventory playerInventory, ServerPlayer player, String questId) {
        this(containerId, playerInventory, player, questId, new SimpleContainer(MENU_SIZE));
    }

    private AdminQuestDetailMenu(int containerId, Inventory playerInventory, ServerPlayer player, String questId, SimpleContainer questContainer) {
        super(MenuType.GENERIC_9x3, containerId, playerInventory, questContainer, ROWS);
        this.player = player;
        this.questId = questId;
        this.questContainer = questContainer;
        refreshDetailSlots();
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player clickingPlayer) {
        if (slotId >= 0 && slotId < MENU_SIZE) {
            if (!service.hasAdminPermission(player)) {
                player.sendSystemMessage(Component.literal("QuestAdmin: この操作にはOP権限レベル2以上が必要です。").withStyle(ChatFormatting.RED));
                player.closeContainer();
                return;
            }

            if (slotId == BACK_SLOT && clickType == ClickType.PICKUP) {
                AdminQuestMenuProvider.openQuestList(player);
                return;
            }

            if (slotId == TOGGLE_SLOT && clickType == ClickType.PICKUP) {
                sendResult(service.toggleEnabled(player, questId));
                refreshDetailSlots();
                broadcastChanges();
                return;
            }

            if (slotId == DELETE_SLOT && clickType == ClickType.PICKUP) {
                AdminQuestMenuProvider.openDeleteConfirm(player, questId);
            }
            return;
        }

        super.clicked(slotId, button, clickType, clickingPlayer);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player == this.player && !player.isRemoved();
    }

    private void refreshDetailSlots() {
        questContainer.clearContent();
        ItemStack filler = AdminQuestMenuItemFactory.createFillerItem();
        for (int slot = 0; slot < MENU_SIZE; slot++) {
            questContainer.setItem(slot, filler.copy());
        }

        if (!service.hasAdminPermission(player)) {
            questContainer.setItem(13, AdminQuestMenuItemFactory.createNoPermissionItem());
            return;
        }

        QuestDefinition quest = service.findQuest(questId).orElse(null);
        if (quest == null) {
            questContainer.setItem(DETAIL_SLOT, AdminQuestMenuItemFactory.createMissingQuestItem(questId));
            questContainer.setItem(BACK_SLOT, AdminQuestMenuItemFactory.createBackItem());
            return;
        }

        questContainer.setItem(DETAIL_SLOT, AdminQuestMenuItemFactory.createDetailItem(quest));
        questContainer.setItem(TOGGLE_SLOT, AdminQuestMenuItemFactory.createToggleItem(quest));
        questContainer.setItem(DELETE_SLOT, AdminQuestMenuItemFactory.createDeleteItem(quest));
        questContainer.setItem(BACK_SLOT, AdminQuestMenuItemFactory.createBackItem());
    }

    private void sendResult(AdminQuestGuiService.ActionResult result) {
        Component message = Component.literal(result.message());
        player.sendSystemMessage(result.success() ? message : message.copy().withStyle(ChatFormatting.RED));
    }
}
