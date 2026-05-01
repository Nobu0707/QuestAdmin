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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class QuestMenu extends ChestMenu {
    private static final int ROWS = 6;
    private static final int MENU_SIZE = ROWS * 9;

    private final ServerPlayer player;
    private final SimpleContainer questContainer;
    private final QuestGuiService service = new QuestGuiService();
    private final Map<Integer, String> slotQuestIds = new HashMap<>();

    public QuestMenu(int containerId, Inventory playerInventory, ServerPlayer player) {
        this(containerId, playerInventory, player, new SimpleContainer(MENU_SIZE));
    }

    private QuestMenu(int containerId, Inventory playerInventory, ServerPlayer player, SimpleContainer questContainer) {
        super(MenuType.GENERIC_9x6, containerId, playerInventory, questContainer, ROWS);
        this.player = player;
        this.questContainer = questContainer;
        refreshQuestSlots();
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player clickingPlayer) {
        if (slotId >= 0 && slotId < MENU_SIZE) {
            String questId = slotQuestIds.get(slotId);
            if (questId == null) {
                return;
            }

            if (clickType == ClickType.PICKUP && button == 1) {
                QuestMenuProvider.openQuestDetail(player, questId);
                return;
            }

            if (clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE) {
                handleQuestAction(questId);
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

    private void handleQuestAction(String questId) {
        QuestGuiService.ActionResult result = service.activateQuest(player, questId);
        Component message = Component.literal(result.message());
        player.sendSystemMessage(result.success() ? message : message.copy().withStyle(ChatFormatting.RED));
        if (result.hasSecondaryMessage()) {
            player.sendSystemMessage(Component.literal(result.secondaryMessage()));
        }

        if (result.refresh()) {
            refreshQuestSlots();
            broadcastChanges();
        }
    }

    private void refreshQuestSlots() {
        slotQuestIds.clear();
        questContainer.clearContent();

        List<QuestDefinition> quests = service.getVisibleQuests();
        if (quests.isEmpty()) {
            questContainer.setItem(22, QuestMenuItemFactory.createEmptyListItem());
            return;
        }

        int slot = 0;
        for (QuestDefinition quest : quests) {
            if (slot >= MENU_SIZE) {
                break;
            }

            QuestStatus status = service.getStatus(player, quest);
            questContainer.setItem(slot, QuestMenuItemFactory.createListItem(player, quest, status));
            slotQuestIds.put(slot, quest.getId());
            slot++;
        }
    }
}
