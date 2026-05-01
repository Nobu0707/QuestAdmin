package net.nobu0707.questadmin.economy;

import java.util.UUID;

public final class DummyEconomyBridge implements EconomyBridge {
    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public boolean deposit(UUID playerUuid, long amount) {
        return false;
    }

    @Override
    public String getCurrencyName() {
        return "money";
    }

    @Override
    public String getBridgeName() {
        return "DummyEconomyBridge";
    }
}
