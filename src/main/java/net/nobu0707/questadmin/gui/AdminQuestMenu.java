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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AdminQuestMenu extends ChestMenu {
    private static final int ROWS = 6;
    private static final int MENU_SIZE = ROWS * 9;
    private static final int QUEST_SLOT_LIMIT = 45;
    private static final int CREATE_SLOT = 49;

    private final ServerPlayer player;
    private final SimpleContainer questContainer;
    private final AdminQuestGuiService service = new AdminQuestGuiService();
    private final Map<Integer, String> slotQuestIds = new HashMap<>();

    public AdminQuestMenu(int containerId, Inventory playerInventory, ServerPlayer player) {
        this(containerId, playerInventory, player, new SimpleContainer(MENU_SIZE));
    }

    private AdminQuestMenu(int containerId, Inventory playerInventory, ServerPlayer player, SimpleContainer questContainer) {
        super(MenuType.GENERIC_9x6, containerId, playerInventory, questContainer, ROWS);
        this.player = player;
        this.questContainer = questContainer;
        refreshQuestSlots();
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player clickingPlayer) {
        if (slotId >= 0 && slotId < MENU_SIZE) {
            if (!service.hasAdminPermission(player)) {
                player.sendSystemMessage(Component.literal("QuestAdmin: この操作にはOP権限レベル2以上が必要です。").withStyle(ChatFormatting.RED));
                player.closeContainer();
                return;
            }

            if (slotId == CREATE_SLOT && clickType == ClickType.PICKUP) {
                sendResult(service.showCreateNotImplemented(player));
                return;
            }

            String questId = slotQuestIds.get(slotId);
            if (questId == null) {
                return;
            }

            if (clickType == ClickType.QUICK_MOVE) {
                AdminQuestMenuProvider.openDeleteConfirm(player, questId);
                return;
            }

            if (clickType == ClickType.PICKUP && button == 1) {
                AdminQuestMenuProvider.openQuestDetail(player, questId);
                return;
            }

            if (clickType == ClickType.PICKUP && button == 0) {
                sendResult(service.toggleEnabled(player, questId));
                refreshQuestSlots();
                broadcastChanges();
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

    private void refreshQuestSlots() {
        slotQuestIds.clear();
        questContainer.clearContent();

        if (!service.hasAdminPermission(player)) {
            questContainer.setItem(22, AdminQuestMenuItemFactory.createNoPermissionItem());
            return;
        }

        List<QuestDefinition> quests = service.getAllQuests();
        if (quests.isEmpty()) {
            questContainer.setItem(22, AdminQuestMenuItemFactory.createEmptyListItem());
        }

        int slot = 0;
        for (QuestDefinition quest : quests) {
            if (slot >= QUEST_SLOT_LIMIT) {
                break;
            }

            questContainer.setItem(slot, AdminQuestMenuItemFactory.createListItem(quest));
            slotQuestIds.put(slot, quest.getId());
            slot++;
        }

        ItemStack filler = AdminQuestMenuItemFactory.createFillerItem();
        for (int footerSlot = QUEST_SLOT_LIMIT; footerSlot < MENU_SIZE; footerSlot++) {
            questContainer.setItem(footerSlot, filler.copy());
        }
        questContainer.setItem(CREATE_SLOT, AdminQuestMenuItemFactory.createCreateQuestItem());
    }

    private void sendResult(AdminQuestGuiService.ActionResult result) {
        Component message = Component.literal(result.message());
        player.sendSystemMessage(result.success() ? message : message.copy().withStyle(ChatFormatting.RED));
    }
}
