package net.nobu0707.questadmin.quest;

public enum QuestCreationStep {
    QUEST_ID("questId を入力してください。"),
    TITLE("タイトルを入力してください。"),
    DESCRIPTION("説明文を入力してください。空にする場合は - を入力してください。"),
    ITEM_ID("必要アイテムIDを入力してください。例: minecraft:stone"),
    AMOUNT("必要個数を入力してください。"),
    REWARD_MONEY("報酬金額を入力してください。"),
    REPEATABLE("repeatable は現在未対応です。false を入力してください。"),
    ENABLED("有効化しますか？ true / false");

    private final String prompt;

    QuestCreationStep(String prompt) {
        this.prompt = prompt;
    }

    public String getPrompt() {
        return prompt;
    }

    public QuestCreationStep next() {
        QuestCreationStep[] steps = values();
        int nextOrdinal = ordinal() + 1;
        if (nextOrdinal >= steps.length) {
            return null;
        }
        return steps[nextOrdinal];
    }
}
