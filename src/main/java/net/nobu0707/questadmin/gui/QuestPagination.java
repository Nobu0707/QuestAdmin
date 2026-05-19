package net.nobu0707.questadmin.gui;

public final class QuestPagination {
    private QuestPagination() {
    }

    public static int pageCount(int itemCount, int pageSize) {
        if (itemCount <= 0) {
            return 1;
        }
        return (itemCount + pageSize - 1) / pageSize;
    }

    public static int clampPage(int page, int itemCount, int pageSize) {
        int lastPage = pageCount(itemCount, pageSize) - 1;
        if (page < 0) {
            return 0;
        }
        return Math.min(page, lastPage);
    }

    public static int fromIndex(int page, int itemCount, int pageSize) {
        int clampedPage = clampPage(page, itemCount, pageSize);
        return Math.min(clampedPage * pageSize, itemCount);
    }

    public static int toIndex(int page, int itemCount, int pageSize) {
        return Math.min(fromIndex(page, itemCount, pageSize) + pageSize, itemCount);
    }
}
