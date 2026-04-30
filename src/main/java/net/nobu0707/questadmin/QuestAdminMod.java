package net.nobu0707.questadmin;

import net.nobu0707.questadmin.command.QuestCommands;
import net.nobu0707.questadmin.quest.QuestStorage;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod(QuestAdminMod.MOD_ID)
public final class QuestAdminMod {
    public static final String MOD_ID = "questadmin";

    private static QuestStorage questStorage;

    public QuestAdminMod() {
        initializeStorage();
        MinecraftForge.EVENT_BUS.addListener(QuestCommands::register);
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
}
