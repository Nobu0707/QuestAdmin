package net.nobu0707.questadmin.economy;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class EconomyService {
    private final EconomyBridge bridge;

    public EconomyService(EconomyBridge bridge) {
        this.bridge = Objects.requireNonNull(bridge, "bridge");
    }

    public EconomyBridge getBridge() {
        return bridge;
    }

    public boolean isAvailable() {
        return bridge.isAvailable();
    }

    public boolean deposit(UUID playerUuid, long amount) {
        if (amount <= 0 || !isAvailable()) {
            return false;
        }
        return bridge.deposit(playerUuid, amount);
    }

    public String getCurrencyName() {
        return bridge.getCurrencyName();
    }

    public List<String> getStatusLines() {
        return List.of(
                "QuestAdmin Economy:",
                "- bridge: " + bridge.getBridgeName(),
                "- available: " + bridge.isAvailable(),
                "- currency: " + bridge.getCurrencyName()
        );
    }
}
