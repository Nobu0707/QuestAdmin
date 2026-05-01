package net.nobu0707.questadmin.quest;

public enum QuestEditStep {
    TITLE,
    DESCRIPTION,
    ITEM_ID,
    AMOUNT,
    REWARD_MONEY,
    REPEATABLE,
    ENABLED,
    CONFIRM;

    public QuestEditStep next() {
        QuestEditStep[] steps = values();
        int nextOrdinal = ordinal() + 1;
        if (nextOrdinal >= steps.length) {
            return null;
        }
        return steps[nextOrdinal];
    }
}
