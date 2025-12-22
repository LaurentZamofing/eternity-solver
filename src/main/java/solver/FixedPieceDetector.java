package solver;

import model.Board;
import model.Placement;
import util.SaveStateManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * Detects and manages fixed pieces for puzzle solving.
 * Fixed pieces are pre-placed pieces that should not be moved during solving.
 *
 * This class provides two approaches:
 * 1. Detecting fixed pieces from an existing board state
 * 2. Building fixed pieces from saved state during resume
 *
 * Configuration is loaded from puzzle-definitions.properties (Phase 7.1).
 */
public class FixedPieceDetector {

    private static final Properties puzzleConfig = loadPuzzleConfig();

    /**
     * Loads puzzle configuration from properties file.
     * @return Properties object with puzzle definitions
     */
    private static Properties loadPuzzleConfig() {
        Properties props = new Properties();
        try (InputStream input = FixedPieceDetector.class.getClassLoader()
                .getResourceAsStream("puzzle-definitions.properties")) {
            if (input != null) {
                props.load(input);
            }
        } catch (IOException e) {
            // Fallback to defaults if config file not found
            util.SolverLogger.error("Failed to load puzzle-definitions.properties, using defaults");
        }
        return props;
    }

    /**
     * Result of fixed piece detection, containing all necessary information.
     */
    public static class FixedPieceInfo {
        public final int numFixedPieces;
        public final Set<String> fixedPositions;
        public final List<SaveStateManager.PlacementInfo> fixedPiecesList;

        public FixedPieceInfo(int numFixedPieces,
                             Set<String> fixedPositions,
                             List<SaveStateManager.PlacementInfo> fixedPiecesList) {
            this.numFixedPieces = numFixedPieces;
            this.fixedPositions = new HashSet<>(fixedPositions);
            this.fixedPiecesList = new ArrayList<>(fixedPiecesList);
        }
    }

    /**
     * Detects and initializes fixed pieces from board state.
     * Used when starting with pre-placed pieces.
     *
     * @param board The board to scan for placed pieces
     * @param pieceUsed BitSet to mark which pieces are used
     * @return FixedPieceInfo containing all detected fixed pieces
     */
    public FixedPieceInfo detectFromBoard(Board board, BitSet pieceUsed) {
        Set<String> fixedPositions = new HashSet<>();
        List<SaveStateManager.PlacementInfo> fixedPiecesList = new ArrayList<>();

        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                if (!board.isEmpty(r, c)) {
                    fixedPositions.add(r + "," + c);

                    Placement placement = board.getPlacement(r, c);
                    int placedPieceId = placement.getPieceId();
                    int placedRotation = placement.getRotation();
                    pieceUsed.set(placedPieceId);

                    SaveStateManager.PlacementInfo fixedPiece =
                        new SaveStateManager.PlacementInfo(r, c, placedPieceId, placedRotation);

                    fixedPiecesList.add(fixedPiece);
                }
            }
        }

        return new FixedPieceInfo(fixedPiecesList.size(), fixedPositions, fixedPiecesList);
    }

    /**
     * Calculates number of fixed pieces based on puzzle name.
     * Loads configuration from puzzle-definitions.properties (Phase 7.1).
     *
     * @param puzzleName The name of the puzzle
     * @return Expected number of fixed pieces for this puzzle
     */
    public int calculateNumFixedPieces(String puzzleName) {
        // Try exact match first
        String key = "puzzle." + puzzleName + ".fixedPieces";
        String value = puzzleConfig.getProperty(key);

        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                util.SolverLogger.error("Invalid fixed pieces config for " + puzzleName + ": " + value);
            }
        }

        // Try prefix match (e.g., "eternity2_p01" matches "eternity2")
        for (String prop : puzzleConfig.stringPropertyNames()) {
            if (prop.startsWith("puzzle.") && prop.endsWith(".fixedPieces")) {
                String prefix = prop.substring(7, prop.indexOf(".fixedPieces"));
                if (puzzleName.startsWith(prefix)) {
                    try {
                        return Integer.parseInt(puzzleConfig.getProperty(prop));
                    } catch (NumberFormatException e) {
                        // Continue to next
                    }
                }
            }
        }

        // Default fallback
        String defaultValue = puzzleConfig.getProperty("puzzle.default.fixedPieces", "0");
        return Integer.parseInt(defaultValue);
    }

    /**
     * Builds fixed piece information from preloaded placement order.
     * Used when resuming from saved state.
     *
     * @param preloadedOrder The placement order loaded from save file
     * @param numFixedPieces Number of pieces that should be considered fixed
     * @return FixedPieceInfo containing the first N pieces as fixed
     */
    public FixedPieceInfo buildFromPreloadedOrder(
            List<SaveStateManager.PlacementInfo> preloadedOrder,
            int numFixedPieces) {

        Set<String> fixedPositions = new HashSet<>();
        List<SaveStateManager.PlacementInfo> initialFixedPieces = new ArrayList<>();

        for (int i = 0; i < Math.min(numFixedPieces, preloadedOrder.size()); i++) {
            SaveStateManager.PlacementInfo piece = preloadedOrder.get(i);
            initialFixedPieces.add(piece);
            fixedPositions.add(piece.row + "," + piece.col);
        }

        return new FixedPieceInfo(numFixedPieces, fixedPositions, initialFixedPieces);
    }
}
