package net.nobu0707.questadmin.quest;

import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class StorageFileUtil {
    private StorageFileUtil() {
    }

    public static void writeStringSafely(Path target, String content, Logger logger) throws IOException {
        Path parent = target.getParent();
        Path temporaryPath = target.resolveSibling(target.getFileName().toString() + ".tmp");

        try {
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException exception) {
            logger.error("Failed to create parent directory for storage file {}.", target, exception);
            throw exception;
        }

        try {
            Files.writeString(temporaryPath, content, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            logger.error("Failed to write temporary storage file {} for target {}.", temporaryPath, target, exception);
            deleteTemporaryFile(temporaryPath, logger, exception);
            throw exception;
        }

        try {
            moveIntoPlace(temporaryPath, target, logger);
        } catch (IOException exception) {
            logger.error("Failed to replace storage file {} with temporary file {}.", target, temporaryPath, exception);
            deleteTemporaryFile(temporaryPath, logger, exception);
            throw exception;
        }
    }

    private static void moveIntoPlace(Path temporaryPath, Path target, Logger logger) throws IOException {
        try {
            Files.move(
                    temporaryPath,
                    target,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
            );
        } catch (AtomicMoveNotSupportedException exception) {
            logger.warn(
                    "Atomic move is not supported for {}. Falling back to non-atomic replace.",
                    target,
                    exception
            );
            try {
                Files.move(temporaryPath, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException fallbackException) {
                fallbackException.addSuppressed(exception);
                logger.error(
                        "Fallback non-atomic replace failed for {} from temporary file {}.",
                        target,
                        temporaryPath,
                        fallbackException
                );
                throw fallbackException;
            }
        }
    }

    private static void deleteTemporaryFile(Path temporaryPath, Logger logger, IOException originalException) {
        try {
            Files.deleteIfExists(temporaryPath);
        } catch (IOException cleanupException) {
            originalException.addSuppressed(cleanupException);
            logger.warn("Failed to delete temporary storage file {} after save failure.", temporaryPath, cleanupException);
        }
    }
}
