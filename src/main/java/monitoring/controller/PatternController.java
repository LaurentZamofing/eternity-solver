package monitoring.controller;

import monitoring.model.PatternInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * REST Controller for serving pattern images and metadata.
 *
 * <p>Provides endpoints to:
 * <ul>
 *   <li>List all available patterns</li>
 *   <li>Get information about a specific pattern</li>
 *   <li>Serve pattern images (optional, Spring Boot serves static files automatically)</li>
 * </ul>
 *
 * <h2>Endpoints:</h2>
 * <ul>
 *   <li>GET /api/patterns/list - List all 22 patterns</li>
 *   <li>GET /api/patterns/{id} - Get info about a specific pattern</li>
 *   <li>GET /api/patterns/{id}/image - Serve pattern image (optional)</li>
 * </ul>
 *
 * <h2>Static Files:</h2>
 * Pattern images are also served directly by Spring Boot at:
 * <code>/patterns/pattern{1-22}.png</code>
 *
 * @see PatternInfo
 */
@RestController
@RequestMapping("/api/patterns")
@CrossOrigin(origins = "*") // Allow CORS for frontend development
public class PatternController {

    private static final Logger logger = LoggerFactory.getLogger(PatternController.class);

    /**
     * Total number of patterns (1-22, pattern 0 is border/gray with no image).
     */
    private static final int TOTAL_PATTERNS = 22;

    /**
     * Get a list of all available patterns.
     *
     * @return List of PatternInfo objects for patterns 0-22
     */
    @GetMapping("/list")
    public ResponseEntity<List<PatternInfo>> getAllPatterns() {
        logger.debug("Request to list all patterns");

        List<PatternInfo> patterns = new ArrayList<>();

        // Add pattern 0 (border/gray)
        patterns.add(new PatternInfo(0, "border"));

        // Add patterns 1-22
        for (int i = 1; i <= TOTAL_PATTERNS; i++) {
            patterns.add(new PatternInfo(i));
        }

        logger.debug("Returning {} patterns", patterns.size());
        return ResponseEntity.ok(patterns);
    }

    /**
     * Get information about a specific pattern.
     *
     * @param patternId Pattern ID (0-22)
     * @return PatternInfo for the requested pattern
     */
    @GetMapping("/{patternId}")
    public ResponseEntity<PatternInfo> getPattern(@PathVariable int patternId) {
        logger.debug("Request for pattern {}", patternId);

        if (!PatternInfo.isValidPatternId(patternId)) {
            logger.warn("Invalid pattern ID requested: {}", patternId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        PatternInfo pattern = new PatternInfo(patternId);
        return ResponseEntity.ok(pattern);
    }

    /**
     * Serve a pattern image file.
     * <p>
     * Note: This endpoint is optional, as Spring Boot automatically serves
     * files from /static/patterns/ at /patterns/pattern{N}.png
     * </p>
     *
     * @param patternId Pattern ID (1-22, note: 0 has no image)
     * @return Pattern image as PNG
     */
    @GetMapping(value = "/{patternId}/image", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<Resource> getPatternImage(@PathVariable int patternId) {
        logger.debug("Request for pattern {} image", patternId);

        // Pattern 0 (border/gray) has no image file
        if (patternId == 0) {
            logger.warn("Pattern 0 (border) has no image file");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        if (!PatternInfo.isValidPatternId(patternId)) {
            logger.warn("Invalid pattern ID requested: {}", patternId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        try {
            // Load pattern image from static resources
            String imagePath = String.format("static/patterns/pattern%d.png", patternId);
            Resource resource = new ClassPathResource(imagePath);

            if (!resource.exists()) {
                logger.error("Pattern image not found: {}", imagePath);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(resource);

        } catch (Exception e) {
            logger.error("Error loading pattern {} image", patternId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Health check endpoint to verify pattern service is operational.
     *
     * @return Status message
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        // Check if at least one pattern file exists
        try {
            Resource resource = new ClassPathResource("static/patterns/pattern1.png");
            if (resource.exists()) {
                return ResponseEntity.ok("Pattern service is healthy. " + (TOTAL_PATTERNS + 1) + " patterns available.");
            } else {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body("Pattern service is unhealthy. Pattern files not found.");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Pattern service error: " + e.getMessage());
        }
    }

    /**
     * Get pattern statistics and metadata.
     *
     * @return Pattern statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<PatternStats> getStats() {
        PatternStats stats = new PatternStats();
        stats.setTotalPatterns(TOTAL_PATTERNS + 1); // Including pattern 0
        stats.setBorderPatterns(6);  // 0-5
        stats.setInnerPatterns(17);  // 6-22
        stats.setImageFormat("PNG");
        stats.setImageDimensions("256x256");
        return ResponseEntity.ok(stats);
    }

    /**
     * Simple DTO for pattern statistics.
     */
    public static class PatternStats {
        private int totalPatterns;
        private int borderPatterns;
        private int innerPatterns;
        private String imageFormat;
        private String imageDimensions;

        // Getters and Setters
        public int getTotalPatterns() {
            return totalPatterns;
        }

        public void setTotalPatterns(int totalPatterns) {
            this.totalPatterns = totalPatterns;
        }

        public int getBorderPatterns() {
            return borderPatterns;
        }

        public void setBorderPatterns(int borderPatterns) {
            this.borderPatterns = borderPatterns;
        }

        public int getInnerPatterns() {
            return innerPatterns;
        }

        public void setInnerPatterns(int innerPatterns) {
            this.innerPatterns = innerPatterns;
        }

        public String getImageFormat() {
            return imageFormat;
        }

        public void setImageFormat(String imageFormat) {
            this.imageFormat = imageFormat;
        }

        public String getImageDimensions() {
            return imageDimensions;
        }

        public void setImageDimensions(String imageDimensions) {
            this.imageDimensions = imageDimensions;
        }
    }
}
