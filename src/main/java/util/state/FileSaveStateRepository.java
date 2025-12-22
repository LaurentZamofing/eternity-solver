package util.state;

import model.Board;
import model.Piece;
import util.SaveStateManager;
import util.SolverLogger;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * File-based implementation of SaveStateRepository.
 * Uses existing SaveStateIO and SaveFileManager infrastructure.
 *
 * <h2>Storage Format</h2>
 * Saves are stored in: {@code saves/<puzzleName>/<configId>_save_<timestamp>.txt}
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Automatic directory creation</li>
 *   <li>Timestamped save files</li>
 *   <li>Most recent file selection</li>
 *   <li>Total compute time calculation</li>
 * </ul>
 *
 * @author Eternity Solver Team
 * @version 2.0.0 (Phase 7 refactoring)
 */
public class FileSaveStateRepository implements SaveStateRepository {

    private final SaveStateIO saveStateIO;
    private final SaveFileManager fileManager;
    private final String basePath;

    /**
     * Creates repository with default base path ("saves").
     */
    public FileSaveStateRepository() {
        this("saves");
    }

    /**
     * Creates repository with custom base path.
     *
     * @param basePath Base directory for saves
     */
    public FileSaveStateRepository(String basePath) {
        this.basePath = basePath;
        this.saveStateIO = new SaveStateIO();
        this.fileManager = new SaveFileManager();
    }

    @Override
    public void save(SaveState state) throws SaveStateException {
        try {
            String puzzleName = extractPuzzleName(state.getConfigId());
            String puzzleDir = SaveFileManager.getPuzzleSubDir(puzzleName);

            // Create directory if needed
            File dir = new File(puzzleDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // Create filename with timestamp
            long timestamp = System.currentTimeMillis();
            String filename = puzzleDir + state.getConfigId() + "_save_" + timestamp + ".txt";

            // Convert pieces map to unused IDs list
            List<Integer> unusedIds = new ArrayList<>();
            Map<Integer, Piece> pieces = state.getPieces();
            for (int i = 1; i <= pieces.size(); i++) {
                boolean isUsed = false;
                for (int r = 0; r < state.getBoard().getRows(); r++) {
                    for (int c = 0; c < state.getBoard().getCols(); c++) {
                        if (!state.getBoard().isEmpty(r, c)) {
                            if (state.getBoard().getPlacement(r, c).getPieceId() == i) {
                                isUsed = true;
                                break;
                            }
                        }
                    }
                    if (isUsed) break;
                }
                if (!isUsed) {
                    unusedIds.add(i);
                }
            }

            // Delegate to existing SaveStateIO
            SaveStateIO.writeToFile(
                filename,
                puzzleName,
                state.getBoard(),
                state.getDepth(),
                state.getPlacementOrder(),
                unusedIds,
                -1.0, // progressPercentage
                state.getElapsedTimeMs(),
                0, // numFixedPieces
                null // initialFixedPieces
            );

            SolverLogger.info("✓ Saved state: " + state.getConfigId() +
                " (depth=" + state.getDepth() + ", time=" +
                formatTime(state.getElapsedTimeMs()) + ")");

        } catch (Exception e) {
            throw new SaveStateException("Failed to save state: " + state.getConfigId(), e);
        }
    }

    @Override
    public Optional<SaveState> findByConfigId(String configId) {
        try {
            String puzzleName = extractPuzzleName(configId);
            File saveFile = util.SaveStateManager.findCurrentSave(puzzleName);

            if (saveFile == null || !saveFile.exists()) {
                return Optional.empty();
            }

            // Load using existing infrastructure
            util.SaveStateManager.SaveState loaded =
                SaveStateIO.readFromFile(saveFile, puzzleName);

            if (loaded == null) {
                return Optional.empty();
            }

            // Note: Board and Pieces are not fully reconstructed from file alone.
            // The save file only stores piece IDs and rotations, not the actual edge data.
            // To fully restore a board, callers should use SaveStateRepository in conjunction
            // with the original puzzle data to reconstruct Piece objects with edges.
            //
            // For now, we return an empty board - callers should use the placementOrder
            // to reconstruct the board with actual Piece objects.

            Board emptyBoard = new Board(loaded.rows, loaded.cols);
            Map<Integer, Piece> emptyPieces = new HashMap<>();

            // Convert to SaveState
            SaveState state = SaveState.builder()
                .configId(configId)
                .board(emptyBoard)
                .pieces(emptyPieces)
                .placementOrder(loaded.placementOrder)
                .depth(loaded.depth)
                .elapsedTimeMs(loaded.totalComputeTimeMs)
                .timestamp(loaded.timestamp)
                .build();

            return Optional.of(state);

        } catch (Exception e) {
            SolverLogger.info("⚠ Failed to load state: " + configId + " - " + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public boolean exists(String configId) {
        String puzzleName = extractPuzzleName(configId);
        File saveFile = util.SaveStateManager.findCurrentSave(puzzleName);
        return saveFile != null && saveFile.exists();
    }

    @Override
    public boolean delete(String configId) {
        String puzzleName = extractPuzzleName(configId);
        File saveFile = util.SaveStateManager.findCurrentSave(puzzleName);

        if (saveFile != null && saveFile.exists()) {
            boolean deleted = saveFile.delete();
            if (deleted) {
                SolverLogger.info("✓ Deleted save: " + configId);
            }
            return deleted;
        }

        return false;
    }

    @Override
    public List<SaveStateInfo> findAll() {
        List<SaveStateInfo> result = new ArrayList<>();
        File savesDir = new File(basePath);

        if (!savesDir.exists() || !savesDir.isDirectory()) {
            return result;
        }

        // Scan all puzzle directories
        File[] puzzleDirs = savesDir.listFiles(File::isDirectory);
        if (puzzleDirs == null) {
            return result;
        }

        for (File puzzleDir : puzzleDirs) {
            // Scan subdirectories (for new structure: saves/eternity2/eternity2_p01/)
            File[] subDirs = puzzleDir.listFiles(File::isDirectory);
            if (subDirs != null) {
                for (File subDir : subDirs) {
                    scanSaveFiles(subDir, result);
                }
            }

            // Also scan files directly in puzzle directory (for old structure)
            scanSaveFiles(puzzleDir, result);
        }

        return result.stream()
            .sorted(Comparator.comparingLong(SaveStateInfo::getLastModified).reversed())
            .collect(Collectors.toList());
    }

    private void scanSaveFiles(File directory, List<SaveStateInfo> result) {
        File[] saveFiles = directory.listFiles(f ->
            f.isFile() && f.getName().endsWith(".txt") &&
            (f.getName().startsWith("best_") || f.getName().startsWith("current_")));

        if (saveFiles == null) return;

        for (File saveFile : saveFiles) {
            try {
                String configId = extractConfigIdFromFilename(saveFile.getName());
                String puzzleName = directory.getName(); // Use directory name as puzzle name

                util.SaveStateManager.SaveState loaded =
                    SaveStateIO.readFromFile(saveFile, puzzleName);

                if (loaded != null) {
                    result.add(new SaveStateInfo(
                        configId,
                        saveFile,
                        saveFile.lastModified(),
                        loaded.totalComputeTimeMs,
                        loaded.depth
                    ));
                }
            } catch (Exception e) {
                // Skip problematic files
            }
        }
    }

    @Override
    public Optional<File> findMostRecentFile(String configId) {
        String puzzleName = extractPuzzleName(configId);
        File saveFile = util.SaveStateManager.findCurrentSave(puzzleName);
        return Optional.ofNullable(saveFile);
    }

    @Override
    public long getTotalComputeTime(String configId) {
        String puzzleName = extractPuzzleName(configId);
        return util.SaveStateManager.readTotalComputeTime(puzzleName);
    }

    // Helper methods

    private String extractPuzzleName(String configId) {
        // Extract puzzle name from config ID (e.g., "eternity2_p01" -> "eternity2")
        int underscoreIndex = configId.indexOf('_');
        return underscoreIndex > 0 ? configId.substring(0, underscoreIndex) : configId;
    }

    private String extractConfigIdFromFilename(String filename) {
        // Extract config ID from save filename
        // Format: <configId>_save_<timestamp>.txt
        if (filename.contains("_save_")) {
            return filename.substring(0, filename.indexOf("_save_"));
        }
        return filename.replace(".txt", "");
    }

    private String formatTime(long ms) {
        if (ms < 1000) return ms + "ms";
        if (ms < 60000) return String.format("%.1fs", ms / 1000.0);
        return String.format("%.1fm", ms / 60000.0);
    }
}
