package net.nobu0707.questadmin.quest;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class StorageBackupService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final DateTimeFormatter FILE_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final DateTimeFormatter DISPLAY_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int RETENTION_PER_KIND = 10;

    private final Path questsPath;
    private final Path playerQuestsPath;
    private final Path backupDirectory;

    public StorageBackupService(Path questsPath, Path playerQuestsPath) {
        this.questsPath = questsPath;
        this.playerQuestsPath = playerQuestsPath;
        this.backupDirectory = questsPath.getParent().resolve("backups");
    }

    public BackupResult backupNow() {
        String timestamp = LocalDateTime.now().format(FILE_TIMESTAMP_FORMAT);
        List<BackupFileResult> results = new ArrayList<>();
        results.add(backupFile("quests", questsPath, timestamp));
        results.add(backupFile("player_quests", playerQuestsPath, timestamp));
        pruneOldBackups("quests-*.json");
        pruneOldBackups("player_quests-*.json");
        return new BackupResult(List.copyOf(results));
    }

    public List<BackupFileInfo> listBackups(int limit) {
        if (Files.notExists(backupDirectory)) {
            return List.of();
        }

        List<BackupFileInfo> backups = new ArrayList<>();
        collectBackupInfo(backups, "quests-*.json", "quests");
        collectBackupInfo(backups, "player_quests-*.json", "player_quests");
        backups.sort(Comparator.comparing(BackupFileInfo::modifiedTime).reversed()
                .thenComparing(BackupFileInfo::fileName));
        return List.copyOf(backups.subList(0, Math.min(limit, backups.size())));
    }

    public Path backupDirectory() {
        return backupDirectory;
    }

    public int retentionPerKind() {
        return RETENTION_PER_KIND;
    }

    private BackupFileResult backupFile(String kind, Path sourcePath, String timestamp) {
        try {
            if (Files.notExists(sourcePath) || !Files.isRegularFile(sourcePath)) {
                return BackupFileResult.failure(kind, sourcePath.getFileName().toString(), "保存ファイルが存在しません: " + sourcePath);
            }

            Files.createDirectories(backupDirectory);
            Path targetPath = uniqueBackupPath(kind + "-" + timestamp + ".json");
            Files.copy(sourcePath, targetPath, StandardCopyOption.COPY_ATTRIBUTES);
            return BackupFileResult.success(kind, sourcePath.getFileName().toString(), targetPath);
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("QuestAdmin: Failed to back up {}.", sourcePath, exception);
            return BackupFileResult.failure(kind, sourcePath.getFileName().toString(), exception.getMessage());
        }
    }

    private Path uniqueBackupPath(String fileName) {
        Path targetPath = backupDirectory.resolve(fileName);
        if (Files.notExists(targetPath)) {
            return targetPath;
        }

        int extensionIndex = fileName.lastIndexOf(".json");
        String prefix = extensionIndex >= 0 ? fileName.substring(0, extensionIndex) : fileName;
        String suffix = extensionIndex >= 0 ? fileName.substring(extensionIndex) : "";
        for (int index = 1; index < 1000; index++) {
            Path candidate = backupDirectory.resolve(prefix + "-" + index + suffix);
            if (Files.notExists(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("バックアップファイル名を決定できません: " + fileName);
    }

    private void pruneOldBackups(String glob) {
        List<Path> backups = listBackupPaths(glob);
        backups.sort(Comparator.comparing(this::lastModifiedTime).reversed()
                .thenComparing(path -> path.getFileName().toString()));

        for (int index = RETENTION_PER_KIND; index < backups.size(); index++) {
            Path oldBackup = backups.get(index);
            try {
                Files.deleteIfExists(oldBackup);
            } catch (IOException exception) {
                LOGGER.warn("QuestAdmin: Failed to delete old backup {}.", oldBackup, exception);
            }
        }
    }

    private void collectBackupInfo(List<BackupFileInfo> backups, String glob, String kind) {
        for (Path backupPath : listBackupPaths(glob)) {
            try {
                backups.add(new BackupFileInfo(
                        kind,
                        backupPath.getFileName().toString(),
                        Files.size(backupPath),
                        Files.getLastModifiedTime(backupPath)
                ));
            } catch (IOException exception) {
                LOGGER.warn("QuestAdmin: Failed to read backup metadata {}.", backupPath, exception);
            }
        }
    }

    private List<Path> listBackupPaths(String glob) {
        if (Files.notExists(backupDirectory)) {
            return List.of();
        }

        List<Path> backups = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(backupDirectory, glob)) {
            for (Path path : stream) {
                if (Files.isRegularFile(path)) {
                    backups.add(path);
                }
            }
        } catch (IOException exception) {
            LOGGER.warn("QuestAdmin: Failed to list backups in {}.", backupDirectory, exception);
        }
        return backups;
    }

    private FileTime lastModifiedTime(Path path) {
        try {
            return Files.getLastModifiedTime(path);
        } catch (IOException exception) {
            LOGGER.warn("QuestAdmin: Failed to read backup modified time {}.", path, exception);
            return FileTime.fromMillis(0L);
        }
    }

    public record BackupResult(List<BackupFileResult> files) {
        public boolean success() {
            return files.stream().allMatch(BackupFileResult::success);
        }
    }

    public record BackupFileResult(boolean success, String kind, String sourceFileName, Path backupPath, String errorMessage) {
        private static BackupFileResult success(String kind, String sourceFileName, Path backupPath) {
            return new BackupFileResult(true, kind, sourceFileName, backupPath, "");
        }

        private static BackupFileResult failure(String kind, String sourceFileName, String errorMessage) {
            return new BackupFileResult(false, kind, sourceFileName, null, errorMessage == null ? "unknown error" : errorMessage);
        }
    }

    public record BackupFileInfo(String kind, String fileName, long sizeBytes, FileTime modifiedTime) {
        public String formattedModifiedTime() {
            return LocalDateTime.ofInstant(modifiedTime.toInstant(), ZoneId.systemDefault()).format(DISPLAY_TIMESTAMP_FORMAT);
        }
    }
}
