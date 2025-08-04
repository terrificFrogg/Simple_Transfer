package org.simpletransfer.services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.util.function.Consumer;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

public class FolderMonitor {
    private static final Logger logger = LogManager.getLogger();

    private final WatchService watcher;
    private final Path monitoredDir;

    /**
     * Creates a WatchService and registers the given directory.
     *
     * @throws IOException If an I/O error occurs during setup.
     */
    public FolderMonitor(String sourceDir) throws IOException {
        this.watcher = FileSystems.getDefault().newWatchService();
        this.monitoredDir = Paths.get(sourceDir);

        // Validate monitored directory
        if (!Files.exists(monitoredDir) || !Files.isDirectory(monitoredDir)) {
            logger.error("Monitored path is not a valid directory: {}", monitoredDir);
            throw new IllegalArgumentException("Monitored path is not a valid directory: " + monitoredDir);
        }

        // Register the monitored directory for ENTRY_CREATE events only
        this.monitoredDir.register(watcher, ENTRY_CREATE);

        logger.info("Monitoring directory for new files: '{}'", monitoredDir.toAbsolutePath());
    }

    /**
     * Starts the continuous monitoring of the directory.
     */
    public void startMonitoring(Consumer<Path> detectedFile) {
        while (true) {
            WatchKey key;
            try {
                // Retrieve the next queued watch key, waiting indefinitely
                key = watcher.take();
            } catch (InterruptedException x) {
                logger.error("Folder monitoring interrupted. Exiting.");
                Thread.currentThread().interrupt(); // Restore the interrupted status
                return;
            }

            // Iterate over all events for the retrieved key
            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();

                // Handle overflow event (some events might have been lost)
                if (kind == OVERFLOW) {
                    logger.error("Event overflow occurred. Some events might have been lost.");
                    continue;
                }

                // We are only interested in ENTRY_CREATE events
                if (kind == ENTRY_CREATE) {
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path filename = ev.context(); // The name of the created file/directory

                    // Resolve the full path of the newly created file/directory
                    Path createdFilePath = monitoredDir.resolve(filename);

                    // Check if it's a regular file (not a directory)
                    // This is important because WatchService also fires events for directory creation
                    if (Files.isRegularFile(createdFilePath)) {
                        logger.info("[CREATED] Detected new file: {}", createdFilePath.toAbsolutePath());
                        detectedFile.accept(createdFilePath);
                    }
                }
            }

            // Reset the key. If the key is no longer valid, the loop will exit.
            boolean valid = key.reset();
            if (!valid) {
                logger.info("Watch key no longer valid. Monitored directory might have been deleted or unaccessible. Exiting monitoring.");
                break;
            }
        }
    }
}