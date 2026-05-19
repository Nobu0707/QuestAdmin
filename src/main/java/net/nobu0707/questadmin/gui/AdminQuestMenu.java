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
    private static final int PREVIOUS_PAGE_SLOT = 45;
    private static final int REFRESH_SLOT = 48;
    private static final int CREATE_SLOT = 49;
    private static final int PAGE_INFO_SLOT = 50;
    private static final int CLOSE_SLOT = 51;
    private static final int NEXT_PAGE_SLOT = 53;

    private final ServerPlayer player;
    private final SimpleContainer questContainer;
    private final AdminQuestGuiService service = new AdminQuestGuiService();
    private final Map<Integer, String> slotQuestIds = new HashMap<>();
    private int currentPage;

    public AdminQuestMenu(int containerId, Inventory playerInventory, ServerPlayer player) {
        this(containerId, playerInventory, player, 0);
    }

    public AdminQuestMenu(int containerId, Inventory playerInventory, ServerPlayer player, int page) {
        this(containerId, playerInventory, player, page, new SimpleContainer(MENU_SIZE));
    }

    private AdminQuestMenu(int containerId, Inventory playerInventory, ServerPlayer player, int page, SimpleContainer questContainer) {
        super(MenuType.GENERIC_9x6, containerId, playerInventory, questContainer, ROWS);
        this.player = player;
        this.questContainer = questContainer;
        this.currentPage = Math.max(0, page);
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

            if (slotId == PREVIOUS_PAGE_SLOT && clickType == ClickType.PICKUP) {
                changePage(currentPage - 1);
                return;
            }

            if (slotId == NEXT_PAGE_SLOT && clickType == ClickType.PICKUP) {
                changePage(currentPage + 1);
                return;
            }

            if (slotId == REFRESH_SLOT && clickType == ClickType.PICKUP) {
                refreshQuestSlots();
                broadcastChanges();
                return;
            }

            if (slotId == CLOSE_SLOT && clickType == ClickType.PICKUP) {
                player.closeContainer();
                return;
            }

            if (slotId == CREATE_SLOT && clickType == ClickType.PICKUP) {
                sendResult(service.startQuestCreation(player));
                return;
            }

            String questId = slotQuestIds.get(slotId);
            if (questId == null) {
                return;
            }

            if (clickType == ClickType.QUICK_MOVE) {
                if (service.findQuest(questId).isEmpty()) {
                    sendResult(AdminQuestGuiService.ActionResult.failure("QuestAdmin: 存在しないクエストIDです: " + questId));
                    refreshQuestSlots();
                    broadcastChanges();
                    return;
                }

                AdminQuestMenuProvider.openDeleteConfirm(player, questId, currentPage);
                return;
            }

            if (clickType == ClickType.PICKUP && button == 1) {
                if (service.findQuest(questId).isEmpty()) {
                    sendResult(AdminQuestGuiService.ActionResult.failure("QuestAdmin: 存在しないクエストIDです: " + questId));
                    refreshQuestSlots();
                    broadcastChanges();
                    return;
                }

                AdminQuestMenuProvider.openQuestDetail(player, questId, currentPage);
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

    private void changePage(int targetPage) {
        List<QuestDefinition> quests = service.getAllQuests();
        currentPage = QuestPagination.clampPage(targetPage, quests.size(), QUEST_SLOT_LIMIT);
        refreshQuestSlots(quests);
        broadcastChanges();
    }

    private void refreshQuestSlots() {
        refreshQuestSlots(service.getAllQuests());
    }

    private void refreshQuestSlots(List<QuestDefinition> quests) {
        slotQuestIds.clear();
        questContainer.clearContent();

        if (!service.hasAdminPermission(player)) {
            questContainer.setItem(22, AdminQuestMenuItemFactory.createNoPermissionItem());
            return;
        }

        currentPage = QuestPagination.clampPage(currentPage, quests.size(), QUEST_SLOT_LIMIT);
        fillFooter(quests.size());

        if (quests.isEmpty()) {
            questContainer.setItem(22, AdminQuestMenuItemFactory.createEmptyListItem());
        }

        int fromIndex = QuestPagination.fromIndex(currentPage, quests.size(), QUEST_SLOT_LIMIT);
        int toIndex = QuestPagination.toIndex(currentPage, quests.size(), QUEST_SLOT_LIMIT);
        int slot = 0;
        for (int index = fromIndex; index < toIndex; index++) {
            QuestDefinition quest = quests.get(index);
            questContainer.setItem(slot, AdminQuestMenuItemFactory.createListItem(quest));
            slotQuestIds.put(slot, quest.getId());
            slot++;
        }
    }

    private void fillFooter(int totalQuests) {
        ItemStack filler = AdminQuestMenuItemFactory.createFillerItem();
        for (int footerSlot = QUEST_SLOT_LIMIT; footerSlot < MENU_SIZE; footerSlot++) {
            questContainer.setItem(footerSlot, filler.copy());
        }

        int pageCount = QuestPagination.pageCount(totalQuests, QUEST_SLOT_LIMIT);
        questContainer.setItem(PREVIOUS_PAGE_SLOT, AdminQuestMenuItemFactory.createPreviousPageItem(currentPage > 0, currentPage, pageCount));
        questContainer.setItem(REFRESH_SLOT, AdminQuestMenuItemFactory.createRefreshItem());
        questContainer.setItem(CREATE_SLOT, AdminQuestMenuItemFactory.createCreateQuestItem());
        questContainer.setItem(PAGE_INFO_SLOT, AdminQuestMenuItemFactory.createPageInfoItem(currentPage, pageCount, totalQuests));
        questContainer.setItem(CLOSE_SLOT, AdminQuestMenuItemFactory.createCloseItem());
        questContainer.setItem(NEXT_PAGE_SLOT, AdminQuestMenuItemFactory.createNextPageItem(currentPage + 1 < pageCount, currentPage, pageCount));
    }

    private void sendResult(AdminQuestGuiService.ActionResult result) {
        Component message = Component.literal(result.message());
        player.sendSystemMessage(result.success() ? message : message.copy().withStyle(ChatFormatting.RED));
    }
}
