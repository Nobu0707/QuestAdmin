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
import net.nobu0707.questadmin.quest.QuestStatus;

public final class QuestDetailMenu extends ChestMenu {
    private static final int ROWS = 3;
    private static final int MENU_SIZE = ROWS * 9;
    private static final int DETAIL_SLOT = 10;
    private static final int ACTION_SLOT = 13;
    private static final int BACK_SLOT = 16;

    private final ServerPlayer player;
    private final String questId;
    private final SimpleContainer questContainer;
    private final QuestGuiService service = new QuestGuiService();

    public QuestDetailMenu(int containerId, Inventory playerInventory, ServerPlayer player, String questId) {
        this(containerId, playerInventory, player, questId, new SimpleContainer(MENU_SIZE));
    }

    private QuestDetailMenu(int containerId, Inventory playerInventory, ServerPlayer player, String questId, SimpleContainer questContainer) {
        super(MenuType.GENERIC_9x3, containerId, playerInventory, questContainer, ROWS);
        this.player = player;
        this.questId = questId;
        this.questContainer = questContainer;
        refreshDetailSlots();
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player clickingPlayer) {
        if (slotId >= 0 && slotId < MENU_SIZE) {
            if (slotId == BACK_SLOT && clickType == ClickType.PICKUP) {
                QuestMenuProvider.openQuestList(player);
                return;
            }

            if (slotId == ACTION_SLOT && (clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE)) {
                handleQuestAction();
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

    private void handleQuestAction() {
        QuestGuiService.ActionResult result = service.activateQuest(player, questId);
        Component message = Component.literal(result.message());
        player.sendSystemMessage(result.success() ? message : message.copy().withStyle(ChatFormatting.RED));
        if (result.hasSecondaryMessage()) {
            player.sendSystemMessage(Component.literal(result.secondaryMessage()));
        }

        if (result.refresh()) {
            refreshDetailSlots();
            broadcastChanges();
        }
    }

    private void refreshDetailSlots() {
        questContainer.clearContent();
        ItemStack filler = QuestMenuItemFactory.createFillerItem();
        for (int slot = 0; slot < MENU_SIZE; slot++) {
            questContainer.setItem(slot, filler.copy());
        }

        QuestDefinition quest = service.getVisibleQuests().stream()
                .filter(value -> value.getId().equals(questId))
                .findFirst()
                .orElse(null);
        if (quest == null) {
            questContainer.setItem(DETAIL_SLOT, QuestMenuItemFactory.createMissingQuestItem(questId));
            questContainer.setItem(BACK_SLOT, QuestMenuItemFactory.createBackItem());
            return;
        }

        QuestStatus status = service.getStatus(player, quest);
        questContainer.setItem(DETAIL_SLOT, QuestMenuItemFactory.createDetailItem(player, quest, status));
        questContainer.setItem(ACTION_SLOT, QuestMenuItemFactory.createActionItem(quest, status));
        questContainer.setItem(BACK_SLOT, QuestMenuItemFactory.createBackItem());
    }
}
