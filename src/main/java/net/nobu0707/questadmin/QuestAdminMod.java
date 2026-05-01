package net.nobu0707.questadmin;

import net.nobu0707.questadmin.command.QuestCommands;
import net.nobu0707.questadmin.economy.EconomyService;
import net.nobu0707.questadmin.economy.LightmansCurrencyEconomyBridge;
import net.nobu0707.questadmin.quest.PlayerQuestStorage;
import net.nobu0707.questadmin.quest.QuestCreationSessionManager;
import net.nobu0707.questadmin.quest.QuestEditSessionManager;
import net.nobu0707.questadmin.quest.QuestStorage;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod(QuestAdminMod.MOD_ID)
public final class QuestAdminMod {
    public static final String MOD_ID = "questadmin";

    private static QuestStorage questStorage;
    private static PlayerQuestStorage playerQuestStorage;
    private static EconomyService economyService;

    public QuestAdminMod() {
        initializeStorage();
        initializePlayerQuestStorage();
        initializeEconomyService();
        MinecraftForge.EVENT_BUS.addListener(QuestCommands::register);
        MinecraftForge.EVENT_BUS.addListener(QuestCreationSessionManager::onServerChat);
        MinecraftForge.EVENT_BUS.addListener(QuestEditSessionManager::onServerChat);
    }

    public static synchronized QuestStorage initializeStorage() {
        if (questStorage == null) {
            questStorage = QuestStorage.createDefault(FMLPaths.CONFIGDIR.get());
            questStorage.initialize();
        }
        return questStorage;
    }

    public static synchronized QuestStorage getQuestStorage() {
        if (questStorage == null) {
            return initializeStorage();
        }
        return questStorage;
    }

    public static synchronized PlayerQuestStorage initializePlayerQuestStorage() {
        if (playerQuestStorage == null) {
            playerQuestStorage = PlayerQuestStorage.createDefault(FMLPaths.CONFIGDIR.get());
            playerQuestStorage.load();
        }
        return playerQuestStorage;
    }

    public static synchronized PlayerQuestStorage getPlayerQuestStorage() {
        if (playerQuestStorage == null) {
            return initializePlayerQuestStorage();
        }
        return playerQuestStorage;
    }

    public static synchronized EconomyService initializeEconomyService() {
        if (economyService == null) {
            economyService = new EconomyService(new LightmansCurrencyEconomyBridge());
        }
        return economyService;
    }

    public static synchronized EconomyService getEconomyService() {
        if (economyService == null) {
            return initializeEconomyService();
        }
        return economyService;
    }
}
