package util.state;

import model.Board;
import model.Piece;
import util.SaveStateManager;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repository pattern for puzzle save state persistence.
 * Provides clean abstraction over save/load operations.
 *
 * <h2>Repository Pattern Benefits</h2>
 * <ul>
 *   <li>Clean separation: Business logic vs persistence</li>
 *   <li>Testability: Easy to mock for unit tests</li>
 *   <li>Flexibility: Can swap implementations (file, database, cloud)</li>
 *   <li>Single Responsibility: Only handles data access</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>
 * SaveStateRepository repo = new FileSaveStateRepository();
 *
 * // Save state
 * SaveState state = SaveState.builder()
 *     .configId("eternity2_p01")
 *     .board(board)
 *     .pieces(pieces)
 *     .statistics(stats)
 *     .build();
 * repo.save(state);
 *
 * // Load state
 * Optional&lt;SaveState&gt; loaded = repo.findByConfigId("eternity2_p01");
 * if (loaded.isPresent()) {
 *     Board board = loaded.get().getBoard();
 * }
 *
 * // List all saves
 * List&lt;SaveStateInfo&gt; allSaves = repo.findAll();
 * </pre>
 *
 * @author Eternity Solver Team
 * @version 2.0.0 (Phase 7 refactoring)
 */
public interface SaveStateRepository {

    /**
     * Saves puzzle state.
     *
     * @param state State to save
     * @throws SaveStateException if save fails
     */
    void save(SaveState state) throws SaveStateException;

    /**
     * Finds saved state by configuration ID.
     *
     * @param configId Configuration identifier (e.g., "eternity2_p01")
     * @return Optional containing state if found, empty otherwise
     */
    Optional<SaveState> findByConfigId(String configId);

    /**
     * Checks if a save exists for the given configuration.
     *
     * @param configId Configuration identifier
     * @return true if save exists
     */
    boolean exists(String configId);

    /**
     * Deletes saved state.
     *
     * @param configId Configuration identifier
     * @return true if deleted, false if not found
     */
    boolean delete(String configId);

    /**
     * Lists all saved states with metadata.
     *
     * @return List of save state information
     */
    List<SaveStateInfo> findAll();

    /**
     * Gets the most recent save file for a configuration.
     *
     * @param configId Configuration identifier
     * @return Optional containing file if found
     */
    Optional<File> findMostRecentFile(String configId);

    /**
     * Calculates total compute time across all saves for a configuration.
     *
     * @param configId Configuration identifier
     * @return Total milliseconds, or 0 if no saves found
     */
    long getTotalComputeTime(String configId);

    /**
     * Value object containing save state metadata.
     */
    class SaveStateInfo {
        private final String configId;
        private final File file;
        private final long lastModified;
        private final long computeTimeMs;
        private final int depth;

        public SaveStateInfo(String configId, File file, long lastModified,
                           long computeTimeMs, int depth) {
            this.configId = configId;
            this.file = file;
            this.lastModified = lastModified;
            this.computeTimeMs = computeTimeMs;
            this.depth = depth;
        }

        public String getConfigId() { return configId; }
        public File getFile() { return file; }
        public long getLastModified() { return lastModified; }
        public long getComputeTimeMs() { return computeTimeMs; }
        public int getDepth() { return depth; }

        @Override
        public String toString() {
            return String.format("SaveStateInfo{config=%s, depth=%d, time=%dms}",
                configId, depth, computeTimeMs);
        }
    }

    /**
     * Exception thrown when save/load operations fail.
     */
    class SaveStateException extends Exception {
        public SaveStateException(String message) {
            super(message);
        }

        public SaveStateException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
