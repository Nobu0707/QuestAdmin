package net.nobu0707.questadmin.quest;

import java.util.Objects;
import java.util.UUID;

public final class PlayerQuestState {
    private final UUID playerUuid;
    private final String questId;
    private final QuestStatus status;
    private final long completedAt;
    private final long claimedAt;

    public PlayerQuestState(UUID playerUuid, String questId, QuestStatus status, long completedAt, long claimedAt) {
        this.playerUuid = Objects.requireNonNull(playerUuid, "playerUuid");
        this.questId = Objects.requireNonNull(questId, "questId");
        this.status = Objects.requireNonNull(status, "status");
        this.completedAt = completedAt;
        this.claimedAt = claimedAt;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getQuestId() {
        return questId;
    }

    public QuestStatus getStatus() {
        return status;
    }

    public long getCompletedAt() {
        return completedAt;
    }

    public long getClaimedAt() {
        return claimedAt;
    }
}
