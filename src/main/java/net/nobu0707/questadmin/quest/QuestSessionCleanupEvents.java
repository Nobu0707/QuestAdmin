package net.nobu0707.questadmin.quest;

import com.mojang.logging.LogUtils;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;

import java.util.UUID;

public final class QuestSessionCleanupEvents {
    private static final Logger LOGGER = LogUtils.getLogger();

    private QuestSessionCleanupEvents() {
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID playerUuid = event.getEntity().getUUID();
        boolean removedCreationSession = QuestCreationSessionManager.clear(playerUuid);
        boolean removedEditSession = QuestEditSessionManager.clear(playerUuid);
        if (removedCreationSession || removedEditSession) {
            LOGGER.info(
                    "Discarded QuestAdmin session for logged out player {}. creation={}, edit={}",
                    playerUuid,
                    removedCreationSession,
                    removedEditSession
            );
        }
    }

    public static void onServerStopping(ServerStoppingEvent event) {
        int creationSessionCount = QuestCreationSessionManager.clearAll();
        int editSessionCount = QuestEditSessionManager.clearAll();
        if (creationSessionCount > 0 || editSessionCount > 0) {
            LOGGER.info(
                    "Cleared QuestAdmin sessions during server shutdown. creation={}, edit={}",
                    creationSessionCount,
                    editSessionCount
            );
        }
    }
}
