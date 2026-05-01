package net.nobu0707.questadmin.economy;

import java.util.UUID;

public interface EconomyBridge {
    boolean isAvailable();

    boolean deposit(UUID playerUuid, long amount);

    String getCurrencyName();

    String getBridgeName();
}
