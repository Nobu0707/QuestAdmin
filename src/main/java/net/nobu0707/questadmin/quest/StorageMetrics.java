package net.nobu0707.questadmin.quest;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class StorageMetrics {
    private static final Map<StorageKind, MutableStats> STATS = new EnumMap<>(StorageKind.class);
    private static final long WARN_THROTTLE_MS = 30_000L;

    static {
        for (StorageKind kind : StorageKind.values()) {
            STATS.put(kind, new MutableStats(kind.getFileName()));
        }
    }

    private StorageMetrics() {
    }

    public static synchronized StorageWriteStats recordSuccess(StorageKind kind, long durationMs) {
        MutableStats stats = statsFor(kind);
        stats.lastSaveMs = durationMs;
        stats.successCount++;
        return stats.snapshot();
    }

    public static synchronized StorageWriteStats recordFailure(StorageKind kind, long durationMs) {
        MutableStats stats = statsFor(kind);
        stats.lastSaveMs = durationMs;
        stats.failureCount++;
        return stats.snapshot();
    }

    public static synchronized void recordAtomicFallback(StorageKind kind) {
        statsFor(kind).atomicFallbackCount++;
    }

    public static synchronized List<StorageWriteStats> snapshots() {
        List<StorageWriteStats> snapshots = new ArrayList<>();
        for (StorageKind kind : StorageKind.values()) {
            snapshots.add(statsFor(kind).snapshot());
        }
        return List.copyOf(snapshots);
    }

    public static synchronized boolean shouldWarnSlowSave(StorageKind kind, long durationMs, boolean strongWarning) {
        MutableStats stats = statsFor(kind);
        long now = System.currentTimeMillis();
        if (strongWarning) {
            if (now - stats.lastStrongWarningAtMs < WARN_THROTTLE_MS) {
                return false;
            }
            stats.lastStrongWarningAtMs = now;
            return true;
        }

        if (now - stats.lastSlowWarningAtMs < WARN_THROTTLE_MS) {
            return false;
        }
        stats.lastSlowWarningAtMs = now;
        return true;
    }

    private static MutableStats statsFor(StorageKind kind) {
        return STATS.get(kind);
    }

    private static final class MutableStats {
        private final String fileName;
        private long lastSaveMs;
        private long successCount;
        private long failureCount;
        private long atomicFallbackCount;
        private long lastSlowWarningAtMs;
        private long lastStrongWarningAtMs;

        private MutableStats(String fileName) {
            this.fileName = fileName;
        }

        private StorageWriteStats snapshot() {
            return new StorageWriteStats(fileName, lastSaveMs, successCount, failureCount, atomicFallbackCount);
        }
    }
}
