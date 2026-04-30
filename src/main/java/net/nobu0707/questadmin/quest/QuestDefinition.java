package net.nobu0707.questadmin.quest;

import java.util.Objects;

public final class QuestDefinition {
    private final String id;
    private final String title;
    private final String description;
    private final QuestType type;
    private final QuestRequirement requirement;
    private final QuestReward reward;
    private final boolean repeatable;
    private final boolean enabled;
    private final long createdAt;
    private final long updatedAt;

    public QuestDefinition(
            String id,
            String title,
            String description,
            QuestType type,
            QuestRequirement requirement,
            QuestReward reward,
            boolean repeatable,
            boolean enabled,
            long createdAt,
            long updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.title = Objects.requireNonNull(title, "title");
        this.description = Objects.requireNonNull(description, "description");
        this.type = Objects.requireNonNull(type, "type");
        this.requirement = Objects.requireNonNull(requirement, "requirement");
        this.reward = Objects.requireNonNull(reward, "reward");
        this.repeatable = repeatable;
        this.enabled = enabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static QuestDefinition createSample(long now) {
        return new QuestDefinition(
                "wheat_delivery_001",
                "小麦の納品",
                "小麦を64個納品してください。",
                QuestType.ITEM_DELIVERY,
                new QuestRequirement("minecraft:wheat", 64),
                new QuestReward(500),
                false,
                true,
                now,
                now
        );
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public QuestType getType() {
        return type;
    }

    public QuestRequirement getRequirement() {
        return requirement;
    }

    public QuestReward getReward() {
        return reward;
    }

    public boolean isRepeatable() {
        return repeatable;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }
}
