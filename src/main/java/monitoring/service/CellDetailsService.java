package monitoring.service;

import model.Board;
import model.Piece;
import monitoring.model.CellConstraints;
import monitoring.model.CellDetails;
import monitoring.model.PieceOption;
import monitoring.model.PieceDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Service to calculate detailed information about a specific cell in the puzzle.
 * Determines which pieces can fit at a position considering constraints.
 */
@Service
public class CellDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(CellDetailsService.class);

    @Autowired
    private SaveFileParser saveFileParser;

    @Autowired
    private PieceDefinitionService pieceDefinitionService;

    /**
     * Calculate detailed information for a specific cell.
     *
     * @param configName Configuration name (e.g., "eternity2_p11_ascending")
     * @param row Row position (0-indexed)
     * @param col Column position (0-indexed)
     * @return CellDetails with current piece, possibilities, and constraints
     */
    public CellDetails getCellDetails(String configName, int row, int col) {
        try {
            // Find and load the current save file
            Path currentFile = findCurrentSaveFile(configName);
            if (currentFile == null || !Files.exists(currentFile)) {
                logger.warn("No save file found for config: {}", configName);
                return null;
            }

            // Parse save file to get board state
            var metrics = saveFileParser.parseSaveFile(currentFile);
            if (metrics == null || metrics.getBoardState() == null) {
                logger.warn("Could not parse board state from file: {}", currentFile);
                return null;
            }

            return getCellDetailsFromMetrics(metrics, row, col);

        } catch (Exception e) {
            logger.error("Error calculating cell details for {}[{},{}]", configName, row, col, e);
            return null;
        }
    }

    /**
     * Calculate cell details from provided metrics (used for historical views).
     *
     * @param metrics ConfigMetrics with board state
     * @param row Target row
     * @param col Target column
     * @return CellDetails or null if error
     */
    public CellDetails getCellDetailsFromMetrics(monitoring.model.ConfigMetrics metrics, int row, int col) {
        try {
            if (metrics == null || metrics.getBoardState() == null) {
                logger.warn("Invalid metrics provided");
                return null;
            }

            String configName = metrics.getConfigName();
            String puzzleName = extractPuzzleName(configName);

            // Load piece definitions
            Map<Integer, PieceDefinition> pieceDefinitions = pieceDefinitionService.getPieceDefinitions(puzzleName);
            if (pieceDefinitions.isEmpty()) {
                logger.warn("No piece definitions found for puzzle: {}", puzzleName);
                return null;
            }

            // Build board from boardState
            int rows = metrics.getRows();
            int cols = metrics.getCols();
            Board board = buildBoard(metrics.getBoardState(), pieceDefinitions, rows, cols);
            Set<Integer> usedPieces = getUsedPieces(metrics.getBoardState());

            // Calculate cell details
            return calculateCellDetailsCore(metrics.getBoardState(), board, pieceDefinitions,
                                           usedPieces, configName, row, col, rows, cols);

        } catch (Exception e) {
            logger.error("Error calculating cell details from metrics for [{},{}]", row, col, e);
            return null;
        }
    }

    /**
     * Core logic for calculating cell details. Extracted to eliminate duplication.
     */
    private CellDetails calculateCellDetailsCore(String[][] boardState, Board board,
                                                  Map<Integer, PieceDefinition> pieceDefinitions,
                                                  Set<Integer> usedPieces, String configName,
                                                  int row, int col, int rows, int cols) {
        CellDetails details = new CellDetails(row, col);

        // Parse and set current piece
        PieceOption currentPiece = parseCurrentPiece(boardState[row][col], pieceDefinitions);
        details.setCurrentPiece(currentPiece);
        Integer currentPieceId = currentPiece != null ? currentPiece.getPieceId() : null;

        // Calculate constraints
        CellConstraints constraints = calculateConstraints(board, row, col, rows, cols);
        details.setConstraints(constraints);

        // Determine placement strategy
        boolean isAscending = configName.contains("ascending");
        boolean isDescending = configName.contains("descending");

        // Calculate all possible pieces
        PossibilitiesResult result = calculatePossibilities(pieceDefinitions, usedPieces, board,
                                                           row, col, rows, cols, currentPieceId,
                                                           currentPiece, isAscending, isDescending);
        details.setPossiblePieces(result.possibilities);

        // Set statistics
        CellDetails.Statistics stats = createStatistics(pieceDefinitions.size(), usedPieces.size(),
                                                        result.validCount, result.invalidCount);
        details.setStatistics(stats);

        return details;
    }

    /**
     * Parse current piece from cell value.
     */
    private PieceOption parseCurrentPiece(String cellValue, Map<Integer, PieceDefinition> pieceDefinitions) {
        if (cellValue == null || cellValue.isEmpty()) {
            return null;
        }

        String[] parts = cellValue.split("_");
        int pieceId = Integer.parseInt(parts[0]);
        int rotation = Integer.parseInt(parts[1]);

        PieceDefinition pieceDef = pieceDefinitions.get(pieceId);
        int[] edges = rotateEdges(pieceDef, rotation);

        PieceOption current = new PieceOption(pieceId, rotation, edges, true);
        current.setCurrent(true);
        return current;
    }

    /**
     * Result object for possibilities calculation.
     */
    private static class PossibilitiesResult {
        List<PieceOption> possibilities;
        int validCount;
        int invalidCount;

        PossibilitiesResult(List<PieceOption> possibilities, int validCount, int invalidCount) {
            this.possibilities = possibilities;
            this.validCount = validCount;
            this.invalidCount = invalidCount;
        }
    }

    /**
     * Calculate all possible pieces for a cell.
     */
    private PossibilitiesResult calculatePossibilities(Map<Integer, PieceDefinition> pieceDefinitions,
                                                       Set<Integer> usedPieces, Board board,
                                                       int row, int col, int rows, int cols,
                                                       Integer currentPieceId, PieceOption currentPiece,
                                                       boolean isAscending, boolean isDescending) {
        List<PieceOption> possibilities = new ArrayList<>();
        int validCount = 0;
        int invalidCount = 0;

        for (Map.Entry<Integer, PieceDefinition> entry : pieceDefinitions.entrySet()) {
            int pieceId = entry.getKey();

            // Skip if piece is already used (unless it's the current piece)
            if (usedPieces.contains(pieceId)) {
                if (currentPiece == null || currentPiece.getPieceId() != pieceId) {
                    continue;
                }
            }

            PieceDefinition pieceDef = entry.getValue();

            // Test each rotation
            for (int rotation = 0; rotation < 4; rotation++) {
                int[] edges = rotateEdges(pieceDef, rotation);
                boolean isValid = fits(board, row, col, edges, rows, cols);

                PieceOption option = new PieceOption(pieceId, rotation, edges, isValid);

                // Mark if this is the current piece
                if (currentPiece != null &&
                    currentPiece.getPieceId() == pieceId &&
                    currentPiece.getRotation() == rotation) {
                    option.setCurrent(true);
                }

                // Mark if this piece was already tried based on placement strategy
                if (currentPieceId != null) {
                    if (isAscending && pieceId < currentPieceId) {
                        option.setAlreadyTried(true);
                    } else if (isDescending && pieceId > currentPieceId) {
                        option.setAlreadyTried(true);
                    }
                }

                possibilities.add(option);

                if (isValid) {
                    validCount++;
                } else {
                    invalidCount++;
                }
            }
        }

        // Sort: current first, then valid, then invalid
        possibilities.sort((a, b) -> {
            if (a.isCurrent()) return -1;
            if (b.isCurrent()) return 1;
            if (a.isValid() && !b.isValid()) return -1;
            if (!a.isValid() && b.isValid()) return 1;
            return Integer.compare(a.getPieceId(), b.getPieceId());
        });

        return new PossibilitiesResult(possibilities, validCount, invalidCount);
    }

    /**
     * Create statistics object.
     */
    private CellDetails.Statistics createStatistics(int totalPieces, int usedCount,
                                                    int validCount, int invalidCount) {
        return new CellDetails.Statistics(totalPieces, usedCount, validCount, invalidCount);
    }

    /**
     * Find the current save file for a configuration.
     */
    private Path findCurrentSaveFile(String configName) {
        try {
            Path configDir = Paths.get("./saves/eternity2", configName);
            if (!Files.exists(configDir)) {
                return null;
            }

            // Find current_*.txt file (most recent)
            return Files.list(configDir)
                .filter(path -> path.getFileName().toString().startsWith("current_"))
                .max(Comparator.comparing(path -> path.getFileName().toString()))
                .orElse(null);

        } catch (Exception e) {
            logger.error("Error finding save file for {}", configName, e);
            return null;
        }
    }

    /**
     * Extract puzzle name from config name.
     * E.g., "eternity2_p11_ascending" -> "eternity2"
     */
    private String extractPuzzleName(String configName) {
        if (configName.startsWith("eternity2")) {
            return "eternity2";
        } else if (configName.startsWith("indice")) {
            return configName.split("_")[0]; // "indice2", "indice3", etc.
        }
        return configName;
    }

    /**
     * Build a Board object from boardState array.
     */
    private Board buildBoard(String[][] boardState, Map<Integer, PieceDefinition> pieceDefs, int rows, int cols) {
        Board board = new Board(rows, cols);

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                String cellValue = boardState[r][c];
                if (cellValue != null && !cellValue.isEmpty()) {
                    String[] parts = cellValue.split("_");
                    int pieceId = Integer.parseInt(parts[0]);
                    int rotation = Integer.parseInt(parts[1]);

                    PieceDefinition pieceDef = pieceDefs.get(pieceId);
                    if (pieceDef != null) {
                        // Create Piece object
                        int[] edges = new int[]{pieceDef.getNorth(), pieceDef.getEast(),
                                                pieceDef.getSouth(), pieceDef.getWest()};
                        Piece piece = new Piece(pieceId, edges);
                        board.place(r, c, piece, rotation);
                    }
                }
            }
        }

        return board;
    }

    /**
     * Get set of piece IDs that are already used in the board.
     */
    private Set<Integer> getUsedPieces(String[][] boardState) {
        Set<Integer> used = new HashSet<>();
        for (String[] row : boardState) {
            for (String cell : row) {
                if (cell != null && !cell.isEmpty()) {
                    String[] parts = cell.split("_");
                    used.add(Integer.parseInt(parts[0]));
                }
            }
        }
        return used;
    }

    /**
     * Rotate piece edges by k*90° clockwise.
     */
    private int[] rotateEdges(PieceDefinition pieceDef, int k) {
        int[] edges = new int[]{pieceDef.getNorth(), pieceDef.getEast(),
                                pieceDef.getSouth(), pieceDef.getWest()};

        if (k == 0) return edges;

        int[] rotated = new int[4];
        for (int i = 0; i < 4; i++) {
            rotated[i] = edges[(i - k + 4) % 4];
        }
        return rotated;
    }

    /**
     * Calculate constraints for a cell position.
     */
    private CellConstraints calculateConstraints(Board board, int row, int col, int rows, int cols) {
        CellConstraints constraints = new CellConstraints();

        // Check borders
        constraints.setBorder("north", row == 0);
        constraints.setBorder("south", row == rows - 1);
        constraints.setBorder("west", col == 0);
        constraints.setBorder("east", col == cols - 1);

        // Check neighbors
        if (row > 0) {
            var placement = board.getPlacement(row - 1, col);
            if (placement != null) {
                int[] edges = placement.edges;
                constraints.setNeighbor("north",
                    new CellConstraints.NeighborInfo(row - 1, col, placement.getPieceId(),
                                                    placement.getRotation(), edges[2])); // South edge of north neighbor
            }
        }

        if (row < rows - 1) {
            var placement = board.getPlacement(row + 1, col);
            if (placement != null) {
                int[] edges = placement.edges;
                constraints.setNeighbor("south",
                    new CellConstraints.NeighborInfo(row + 1, col, placement.getPieceId(),
                                                    placement.getRotation(), edges[0])); // North edge of south neighbor
            }
        }

        if (col > 0) {
            var placement = board.getPlacement(row, col - 1);
            if (placement != null) {
                int[] edges = placement.edges;
                constraints.setNeighbor("west",
                    new CellConstraints.NeighborInfo(row, col - 1, placement.getPieceId(),
                                                    placement.getRotation(), edges[1])); // East edge of west neighbor
            }
        }

        if (col < cols - 1) {
            var placement = board.getPlacement(row, col + 1);
            if (placement != null) {
                int[] edges = placement.edges;
                constraints.setNeighbor("east",
                    new CellConstraints.NeighborInfo(row, col + 1, placement.getPieceId(),
                                                    placement.getRotation(), edges[3])); // West edge of east neighbor
            }
        }

        return constraints;
    }

    /**
     * Check if piece with given edges fits at position.
     * Simplified version of PlacementValidator.fits().
     */
    private boolean fits(Board board, int row, int col, int[] edges, int rows, int cols) {
        // Check border constraints
        if (row == 0 && edges[0] != 0) return false; // North border
        if (row == rows - 1 && edges[2] != 0) return false; // South border
        if (col == 0 && edges[3] != 0) return false; // West border
        if (col == cols - 1 && edges[1] != 0) return false; // East border

        // Check neighbor constraints
        if (row > 0) {
            var placement = board.getPlacement(row - 1, col);
            if (placement != null && placement.edges[2] != edges[0]) {
                return false; // North neighbor mismatch
            }
        }

        if (row < rows - 1) {
            var placement = board.getPlacement(row + 1, col);
            if (placement != null && placement.edges[0] != edges[2]) {
                return false; // South neighbor mismatch
            }
        }

        if (col > 0) {
            var placement = board.getPlacement(row, col - 1);
            if (placement != null && placement.edges[1] != edges[3]) {
                return false; // West neighbor mismatch
            }
        }

        if (col < cols - 1) {
            var placement = board.getPlacement(row, col + 1);
            if (placement != null && placement.edges[3] != edges[1]) {
                return false; // East neighbor mismatch
            }
        }

        return true;
    }
}
