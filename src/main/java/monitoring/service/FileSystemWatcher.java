package monitoring.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Observer-pattern based file system watcher.
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>WatchService management and lifecycle</li>
 *   <li>Recursive directory registration</li>
 *   <li>Event polling loop in background thread</li>
 *   <li>Notification of file change observers</li>
 * </ul>
 *
 * <h2>Observer Pattern</h2>
 * Uses functional interfaces (Consumer) for loose coupling.
 * Observers are notified when files are created or modified.
 *
 * <h2>Usage</h2>
 * <pre>
 * FileSystemWatcher watcher = new FileSystemWatcher(savesDirectory);
 *
 * watcher.addSaveFileObserver(path -> {
 *     // Handle save file change
 * });
 *
 * watcher.addStatsFileObserver(path -> {
 *     // Handle stats file change
 * });
 *
 * watcher.start();
 * // ...
 * watcher.stop();
 * </pre>
 *
 * @author Eternity Solver Team
 * @version 1.0.0
 */
public class FileSystemWatcher {

    private static final Logger logger = LoggerFactory.getLogger(FileSystemWatcher.class);

    private final String rootDirectory;
    private WatchService watchService;
    private ExecutorService executorService;
    private volatile boolean running = false;

    // Watch keys for each directory
    private final Map<WatchKey, Path> watchKeys = new ConcurrentHashMap<>();

    // Observers (Observer Pattern using functional interfaces)
    private Consumer<Path> saveFileObserver;
    private Consumer<Path> statsFileObserver;
    private Consumer<Path> directoryCreatedObserver;

    /**
     * Creates a file system watcher for the specified root directory.
     *
     * @param rootDirectory Root directory to watch recursively
     */
    public FileSystemWatcher(String rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    /**
     * Sets the observer for save file changes.
     *
     * @param observer Consumer that will be notified of save file changes
     */
    public void setSaveFileObserver(Consumer<Path> observer) {
        this.saveFileObserver = observer;
    }

    /**
     * Sets the observer for stats file changes.
     *
     * @param observer Consumer that will be notified of stats file changes
     */
    public void setStatsFileObserver(Consumer<Path> observer) {
        this.statsFileObserver = observer;
    }

    /**
     * Sets the observer for directory creation events.
     *
     * @param observer Consumer that will be notified of new directories
     */
    public void setDirectoryCreatedObserver(Consumer<Path> observer) {
        this.directoryCreatedObserver = observer;
    }

    /**
     * Starts the file system watcher.
     * Registers all directories recursively and begins event polling loop.
     *
     * @throws IOException if watch service cannot be initialized
     */
    public void start() throws IOException {
        logger.info("Starting FileSystemWatcher for directory: {}", rootDirectory);

        // Create watch service
        watchService = FileSystems.getDefault().newWatchService();

        // Start executor for background processing
        executorService = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "FileSystemWatcher-Thread");
            t.setDaemon(true);
            return t;
        });

        // Register root directory and all subdirectories
        Path rootPath = Paths.get(rootDirectory);
        if (Files.exists(rootPath)) {
            registerDirectory(rootPath);
        } else {
            logger.warn("Root directory does not exist: {}", rootDirectory);
        }

        running = true;

        // Submit watch task to executor
        executorService.submit(this::watchLoop);

        logger.info("✅ FileSystemWatcher started successfully");
    }

    /**
     * Register a directory and all subdirectories for watching.
     *
     * @param dir Directory to register
     * @throws IOException if registration fails
     */
    private void registerDirectory(Path dir) throws IOException {
        if (Files.isDirectory(dir)) {
            WatchKey key = dir.register(
                    watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY
            );
            watchKeys.put(key, dir);
            logger.debug("Registered watch for: {}", dir);

            // Register subdirectories recursively
            Files.list(dir)
                    .filter(Files::isDirectory)
                    .forEach(subDir -> {
                        try {
                            registerDirectory(subDir);
                        } catch (IOException e) {
                            logger.error("Failed to register subdirectory: {}", subDir, e);
                        }
                    });
        }
    }

    /**
     * Main watch loop that polls for file system events.
     * Runs in background thread until stopped.
     */
    private void watchLoop() {
        logger.info("File system watch loop started");

        while (running) {
            try {
                // Wait for events (blocks until events arrive or 1 second timeout)
                WatchKey key = watchService.poll(1, TimeUnit.SECONDS);

                if (key == null) {
                    continue;
                }

                Path dir = watchKeys.get(key);

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }

                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path filename = ev.context();
                    Path fullPath = dir.resolve(filename);

                    // Process file change
                    if (kind == StandardWatchEventKinds.ENTRY_CREATE ||
                            kind == StandardWatchEventKinds.ENTRY_MODIFY) {

                        handleFileChange(fullPath, kind);

                        // If a new directory was created, register it and notify observer
                        if (kind == StandardWatchEventKinds.ENTRY_CREATE &&
                                Files.isDirectory(fullPath)) {
                            registerDirectory(fullPath);
                            if (directoryCreatedObserver != null) {
                                directoryCreatedObserver.accept(fullPath);
                            }
                        }
                    }
                }

                // Reset the key to receive further events
                boolean valid = key.reset();
                if (!valid) {
                    watchKeys.remove(key);
                }

            } catch (InterruptedException e) {
                logger.info("File system watcher interrupted");
                break;
            } catch (Exception e) {
                logger.error("Error in file system watch loop", e);
            }
        }

        logger.info("File system watch loop stopped");
    }

    /**
     * Handles a file change event by notifying appropriate observers.
     *
     * @param path Path to the changed file
     * @param kind Type of change (CREATE or MODIFY)
     */
    private void handleFileChange(Path path, WatchEvent.Kind<?> kind) {
        String fileName = path.getFileName().toString();

        // Check if it's a save file (current_*.txt, best_*.txt)
        if (isSaveFile(fileName)) {
            logger.debug("Save file changed: {} ({})", path, kind.name());
            if (saveFileObserver != null) {
                saveFileObserver.accept(path);
            }
        }
        // Check if it's a stats file (stats_history.jsonl)
        else if (isStatsFile(fileName)) {
            logger.debug("Stats file changed: {} ({})", path, kind.name());
            if (statsFileObserver != null) {
                statsFileObserver.accept(path);
            }
        }
    }

    /**
     * Checks if a file name is a save file.
     *
     * @param fileName File name to check
     * @return true if save file (current_*.txt or best_*.txt)
     */
    private boolean isSaveFile(String fileName) {
        return (fileName.startsWith("current_") || fileName.startsWith("best_")) &&
               fileName.endsWith(".txt");
    }

    /**
     * Checks if a file name is a stats file.
     *
     * @param fileName File name to check
     * @return true if stats file (stats_history*.jsonl)
     */
    private boolean isStatsFile(String fileName) {
        return fileName.equals("stats_history.jsonl") ||
               fileName.matches("stats_history_\\d+\\.jsonl");
    }

    /**
     * Stops the file system watcher.
     * Shuts down executor and closes watch service.
     *
     * @param timeoutSeconds Maximum time to wait for clean shutdown
     */
    public void stop(long timeoutSeconds) {
        logger.info("Stopping FileSystemWatcher...");
        running = false;

        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
            }
        }

        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                logger.error("Error closing watch service", e);
            }
        }

        logger.info("✅ FileSystemWatcher stopped");
    }

    /**
     * Returns whether the watcher is currently running.
     *
     * @return true if running, false otherwise
     */
    public boolean isRunning() {
        return running;
    }
}
