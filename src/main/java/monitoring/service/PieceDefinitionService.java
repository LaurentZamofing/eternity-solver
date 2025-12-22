package monitoring.service;

import monitoring.model.PieceDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to load and cache piece definitions from puzzle files.
 * Piece definitions are stored in data/eternity2/ directory.
 */
@Service
public class PieceDefinitionService {

    private static final Logger logger = LoggerFactory.getLogger(PieceDefinitionService.class);

    @Value("${monitoring.data-directory:./data}")
    private String dataDirectory;

    // Cache: puzzleName -> (pieceId -> PieceDefinition)
    private final Map<String, Map<Integer, PieceDefinition>> cache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        logger.info("Initializing PieceDefinitionService with data directory: {}", dataDirectory);
    }

    /**
     * Get piece definitions for a puzzle configuration.
     *
     * @param puzzleName Name like "eternity2_p01_ascending"
     * @return Map of pieceId -> PieceDefinition
     */
    public Map<Integer, PieceDefinition> getPieceDefinitions(String puzzleName) {
        // Check cache first
        if (cache.containsKey(puzzleName)) {
            return cache.get(puzzleName);
        }

        // Load from file
        Map<Integer, PieceDefinition> definitions = loadPieceDefinitions(puzzleName);

        if (definitions != null && !definitions.isEmpty()) {
            cache.put(puzzleName, definitions);
            logger.info("Loaded {} piece definitions for puzzle: {}", definitions.size(), puzzleName);
        }

        return definitions != null ? definitions : new HashMap<>();
    }

    /**
     * Get a specific piece definition.
     */
    public PieceDefinition getPieceDefinition(String puzzleName, int pieceId) {
        Map<Integer, PieceDefinition> definitions = getPieceDefinitions(puzzleName);
        return definitions.get(pieceId);
    }

    /**
     * Load piece definitions from file.
     * Format: each line is "north east south west"
     * Line number corresponds to piece ID (1-indexed).
     */
    private Map<Integer, PieceDefinition> loadPieceDefinitions(String puzzleName) {
        Map<Integer, PieceDefinition> definitions = new HashMap<>();

        // Try multiple possible file locations
        String[] possiblePaths = {
            dataDirectory + "/eternity2/" + puzzleName + ".txt",
            dataDirectory + "/archives/eternity2_256_pieces.txt",
            "./data/eternity2/" + puzzleName + ".txt",
            "./data/archives/eternity2_256_pieces.txt"
        };

        File puzzleFile = null;
        for (String path : possiblePaths) {
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                puzzleFile = file;
                logger.debug("Found puzzle file: {}", path);
                break;
            }
        }

        if (puzzleFile == null) {
            logger.warn("Could not find puzzle definition file for: {}", puzzleName);
            return definitions;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(puzzleFile))) {
            String line;
            int pieceId = 1;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Skip empty lines and comments
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // Parse line: can be "north east south west" (4 cols) or "pieceId north east south west extra" (5+ cols)
                String[] parts = line.split("\\s+");
                if (parts.length >= 4) {
                    try {
                        // If 5+ columns, format is: pieceId N E S W [extra]
                        // If 4 columns, format is: N E S W
                        int offset = (parts.length >= 5) ? 1 : 0;
                        int north = Integer.parseInt(parts[offset + 0]);
                        int east = Integer.parseInt(parts[offset + 1]);
                        int south = Integer.parseInt(parts[offset + 2]);
                        int west = Integer.parseInt(parts[offset + 3]);

                        PieceDefinition piece = new PieceDefinition(pieceId, north, east, south, west);
                        definitions.put(pieceId, piece);
                        pieceId++;
                    } catch (NumberFormatException e) {
                        logger.warn("Invalid piece definition on line {}: {}", pieceId, line);
                    }
                }
            }

            logger.info("Loaded {} pieces from {}", definitions.size(), puzzleFile.getName());

        } catch (IOException e) {
            logger.error("Failed to load puzzle definitions from {}", puzzleFile.getAbsolutePath(), e);
        }

        return definitions;
    }

    /**
     * Clear cache for a specific puzzle.
     */
    public void clearCache(String puzzleName) {
        cache.remove(puzzleName);
        logger.debug("Cleared cache for puzzle: {}", puzzleName);
    }

    /**
     * Clear all cached definitions.
     */
    public void clearAll() {
        cache.clear();
        logger.info("Cleared all piece definition caches");
    }

    /**
     * Get list of available puzzles.
     */
    public Set<String> getAvailablePuzzles() {
        return new HashSet<>(cache.keySet());
    }
}
