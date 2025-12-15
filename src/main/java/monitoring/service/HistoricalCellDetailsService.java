package monitoring.service;

import monitoring.model.ConfigMetrics;
import monitoring.model.PlacementInfo;
import monitoring.model.CellDetails;
import monitoring.model.PieceDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;

/**
 * Service for generating historical cell details.
 * Reconstructs board state at the time a specific piece was placed
 * to show what options were available without later constraints.
 */
@Service
public class HistoricalCellDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(HistoricalCellDetailsService.class);

    private final SaveFileParser saveFileParser;
    private final PieceDefinitionService pieceDefinitionService;
    private final CellDetailsService cellDetailsService;

    public HistoricalCellDetailsService(
            SaveFileParser saveFileParser,
            PieceDefinitionService pieceDefinitionService,
            CellDetailsService cellDetailsService) {
        this.saveFileParser = saveFileParser;
        this.pieceDefinitionService = pieceDefinitionService;
        this.cellDetailsService = cellDetailsService;
    }

    /**
     * Get cell details as they were at the time the piece was placed.
     * Reconstructs board state up to (but not including) the target cell.
     *
     * @param configName Configuration name
     * @param row Target row
     * @param col Target column
     * @return Historical cell details, or null if not available
     */
    public CellDetails getHistoricalCellDetails(String configName, int row, int col) {

        try {
            // Find and parse the latest save file for this config
            ConfigMetrics metrics = findLatestSaveFile(configName);
            if (metrics == null) {
                logger.warn("No save file found for config: {}", configName);
                return null;
            }

            // Get placement order
            List<PlacementInfo> placementOrder = metrics.getPlacementOrder();
            if (placementOrder == null || placementOrder.isEmpty()) {
                logger.warn("No placement order available for config: {}", configName);
                return null;
            }

            // Find the target cell in placement order
            int targetSequence = -1;
            for (int i = 0; i < placementOrder.size(); i++) {
                PlacementInfo info = placementOrder.get(i);
                if (info.getRow() == row && info.getCol() == col) {
                    targetSequence = i;
                    break;
                }
            }

            if (targetSequence == -1) {
                logger.warn("Cell ({},{}) not found in placement order for config: {}", row, col, configName);
                return null;
            }

            // Reconstruct board state up to (but not including) the target cell
            String[][] historicalBoard = new String[metrics.getRows()][metrics.getCols()];

            // Place all pieces that were placed BEFORE the target cell
            for (int i = 0; i < targetSequence; i++) {
                PlacementInfo info = placementOrder.get(i);
                historicalBoard[info.getRow()][info.getCol()] =
                    info.getPieceId() + "_" + info.getRotation();
            }

            // Create a temporary ConfigMetrics with the historical board state
            ConfigMetrics historicalMetrics = new ConfigMetrics();
            historicalMetrics.setConfigName(configName);
            historicalMetrics.setRows(metrics.getRows());
            historicalMetrics.setCols(metrics.getCols());
            historicalMetrics.setBoardState(historicalBoard);

            // Use the existing CellDetailsService logic but with historical board
            return cellDetailsService.getCellDetailsFromMetrics(historicalMetrics, row, col);

        } catch (RuntimeException e) {
            logger.error("Error generating historical cell details for {} at ({},{})",
                configName, row, col, e);
            return null;
        }
    }

    /**
     * Find the latest save file for a configuration.
     */
    private ConfigMetrics findLatestSaveFile(String configName) {
        // Try to find save file in saves directory
        String basePath = "saves/" + extractPuzzleName(configName) + "/" + configName;
        File configDir = new File(basePath);

        if (!configDir.exists() || !configDir.isDirectory()) {
            logger.warn("Config directory not found: {}", basePath);
            return null;
        }

        // Find all save files (current_*.txt and best_*.txt)
        File[] saveFiles = configDir.listFiles((dir, name) ->
            name.matches("(current|best)_\\d+\\.txt")
        );

        if (saveFiles == null || saveFiles.length == 0) {
            logger.warn("No save files found in: {}", basePath);
            return null;
        }

        // Sort by last modified time (newest first)
        Arrays.sort(saveFiles, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));

        // Parse the newest file
        return saveFileParser.parseSaveFile(saveFiles[0]);
    }

    /**
     * Extract base puzzle name from config name.
     * Examples:
     * - "eternity2_p01_ascending" -> "eternity2"
     * - "indice2_ascending" -> "indice2"
     */
    private String extractPuzzleName(String configName) {
        if (configName.startsWith("eternity2")) {
            return "eternity2";
        } else if (configName.startsWith("indice")) {
            // Extract indice2, indice3, etc.
            int underscoreIdx = configName.indexOf('_');
            if (underscoreIdx > 0) {
                return configName.substring(0, underscoreIdx);
            }
        }
        return configName;
    }
}
