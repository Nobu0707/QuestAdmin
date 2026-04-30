package net.nobu0707.questadmin.quest;

import java.util.Objects;

public final class QuestRequirement {
    private final String itemId;
    private final int amount;

    public QuestRequirement(String itemId, int amount) {
        this.itemId = Objects.requireNonNull(itemId, "itemId");
        this.amount = amount;
    }

    public String getItemId() {
        return itemId;
    }

    public int getAmount() {
        return amount;
    }
}
