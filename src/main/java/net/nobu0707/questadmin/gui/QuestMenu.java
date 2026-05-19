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
    private static final int QUEST_SLOT_LIMIT = 45;
    private static final int PREVIOUS_PAGE_SLOT = 45;
    private static final int REFRESH_SLOT = 48;
    private static final int PAGE_INFO_SLOT = 49;
    private static final int CLOSE_SLOT = 50;
    private static final int NEXT_PAGE_SLOT = 53;

    private final ServerPlayer player;
    private final SimpleContainer questContainer;
    private final QuestGuiService service = new QuestGuiService();
    private final Map<Integer, String> slotQuestIds = new HashMap<>();
    private int currentPage;

    public QuestMenu(int containerId, Inventory playerInventory, ServerPlayer player) {
        this(containerId, playerInventory, player, 0);
    }

    public QuestMenu(int containerId, Inventory playerInventory, ServerPlayer player, int page) {
        this(containerId, playerInventory, player, page, new SimpleContainer(MENU_SIZE));
    }

    private QuestMenu(int containerId, Inventory playerInventory, ServerPlayer player, int page, SimpleContainer questContainer) {
        super(MenuType.GENERIC_9x6, containerId, playerInventory, questContainer, ROWS);
        this.player = player;
        this.questContainer = questContainer;
        this.currentPage = Math.max(0, page);
        refreshQuestSlots();
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player clickingPlayer) {
        if (slotId >= 0 && slotId < MENU_SIZE) {
            if (clickType == ClickType.PICKUP && slotId == PREVIOUS_PAGE_SLOT) {
                changePage(currentPage - 1);
                return;
            }

            if (clickType == ClickType.PICKUP && slotId == NEXT_PAGE_SLOT) {
                changePage(currentPage + 1);
                return;
            }

            if (clickType == ClickType.PICKUP && slotId == REFRESH_SLOT) {
                refreshQuestSlots();
                broadcastChanges();
                return;
            }

            if (clickType == ClickType.PICKUP && slotId == CLOSE_SLOT) {
                player.closeContainer();
                return;
            }

            String questId = slotQuestIds.get(slotId);
            if (questId == null) {
                return;
            }

            if (clickType == ClickType.PICKUP && button == 1) {
                if (service.findVisibleQuest(questId).isEmpty()) {
                    player.sendSystemMessage(Component.literal("QuestAdmin: 存在しない、または無効なクエストです: " + questId).withStyle(ChatFormatting.RED));
                    refreshQuestSlots();
                    broadcastChanges();
                    return;
                }

                QuestMenuProvider.openQuestDetail(player, questId, currentPage);
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

    private void changePage(int targetPage) {
        List<QuestDefinition> quests = service.getVisibleQuests();
        currentPage = QuestPagination.clampPage(targetPage, quests.size(), QUEST_SLOT_LIMIT);
        refreshQuestSlots(quests);
        broadcastChanges();
    }

    private void refreshQuestSlots() {
        refreshQuestSlots(service.getVisibleQuests());
    }

    private void refreshQuestSlots(List<QuestDefinition> quests) {
        slotQuestIds.clear();
        questContainer.clearContent();

        currentPage = QuestPagination.clampPage(currentPage, quests.size(), QUEST_SLOT_LIMIT);
        fillFooter(quests.size());

        if (quests.isEmpty()) {
            questContainer.setItem(22, QuestMenuItemFactory.createEmptyListItem());
            return;
        }

        InventoryItemCountSnapshot itemCounts = InventoryItemCountSnapshot.capture(player);
        int fromIndex = QuestPagination.fromIndex(currentPage, quests.size(), QUEST_SLOT_LIMIT);
        int toIndex = QuestPagination.toIndex(currentPage, quests.size(), QUEST_SLOT_LIMIT);
        int slot = 0;
        for (int index = fromIndex; index < toIndex; index++) {
            QuestDefinition quest = quests.get(index);
            QuestStatus status = service.getStatus(player, quest);
            questContainer.setItem(slot, QuestMenuItemFactory.createListItem(quest, status, itemCounts));
            slotQuestIds.put(slot, quest.getId());
            slot++;
        }
    }

    private void fillFooter(int totalQuests) {
        ItemStack filler = QuestMenuItemFactory.createFillerItem();
        for (int footerSlot = QUEST_SLOT_LIMIT; footerSlot < MENU_SIZE; footerSlot++) {
            questContainer.setItem(footerSlot, filler.copy());
        }

        int pageCount = QuestPagination.pageCount(totalQuests, QUEST_SLOT_LIMIT);
        questContainer.setItem(PREVIOUS_PAGE_SLOT, QuestMenuItemFactory.createPreviousPageItem(currentPage > 0, currentPage, pageCount));
        questContainer.setItem(REFRESH_SLOT, QuestMenuItemFactory.createRefreshItem());
        questContainer.setItem(PAGE_INFO_SLOT, QuestMenuItemFactory.createPageInfoItem(currentPage, pageCount, totalQuests));
        questContainer.setItem(CLOSE_SLOT, QuestMenuItemFactory.createCloseItem());
        questContainer.setItem(NEXT_PAGE_SLOT, QuestMenuItemFactory.createNextPageItem(currentPage + 1 < pageCount, currentPage, pageCount));
    }
}
