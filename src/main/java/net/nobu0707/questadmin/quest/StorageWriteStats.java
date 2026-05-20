package net.nobu0707.questadmin.quest;

public record StorageWriteStats(
        String fileName,
        long lastSaveMs,
        long successCount,
        long failureCount,
        long atomicFallbackCount
) {
}
