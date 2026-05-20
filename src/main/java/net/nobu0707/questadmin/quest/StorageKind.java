package net.nobu0707.questadmin.quest;

public enum StorageKind {
    QUESTS("quests.json"),
    PLAYER_QUESTS("player_quests.json");

    private final String fileName;

    StorageKind(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }
}
