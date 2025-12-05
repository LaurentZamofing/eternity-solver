package monitoring.model;

/**
 * Information about a pattern image used for puzzle piece visualization.
 *
 * <p>Pattern images are 256x256 PNG files that represent the visual appearance
 * of puzzle piece edges. Each piece has 4 edges (North, East, South, West),
 * and each edge displays one of these patterns.</p>
 *
 * <h2>Pattern Types:</h2>
 * <ul>
 *   <li><b>Pattern 0</b>: Border (gray, no pattern - edges facing outside)</li>
 *   <li><b>Patterns 1-5</b>: Border patterns (used on edge pieces)</li>
 *   <li><b>Patterns 6-22</b>: Inner patterns (used on interior pieces)</li>
 * </ul>
 *
 * <h2>Usage:</h2>
 * <pre>
 * PatternInfo pattern = new PatternInfo(1, "border");
 * String url = pattern.getUrl(); // "/patterns/pattern1.png"
 * </pre>
 *
 * @see <a href="https://github.com/TheSil/edge_puzzle">Edge Puzzle Project</a>
 */
public class PatternInfo {

    /**
     * Pattern ID (0-22).
     * - 0 = Border/Gray (no pattern)
     * - 1-5 = Border patterns
     * - 6-22 = Inner patterns
     */
    private int id;

    /**
     * URL path to the pattern image.
     * Format: "/patterns/pattern{id}.png"
     */
    private String url;

    /**
     * Pattern type: "border" (0-5) or "inner" (6-22)
     */
    private String type;

    /**
     * Human-readable pattern name.
     * Format: "Pattern {id}" or "Border" for id=0
     */
    private String name;

    /**
     * Default constructor for JSON serialization.
     */
    public PatternInfo() {
    }

    /**
     * Creates a PatternInfo with the given ID.
     * Automatically determines type and generates URL.
     *
     * @param id Pattern ID (0-22)
     */
    public PatternInfo(int id) {
        this.id = id;
        this.url = (id == 0) ? null : String.format("/patterns/pattern%d.png", id);
        this.type = determineType(id);
        this.name = (id == 0) ? "Border" : String.format("Pattern %d", id);
    }

    /**
     * Creates a PatternInfo with explicit type.
     *
     * @param id Pattern ID (0-22)
     * @param type Pattern type ("border" or "inner")
     */
    public PatternInfo(int id, String type) {
        this.id = id;
        this.url = (id == 0) ? null : String.format("/patterns/pattern%d.png", id);
        this.type = type;
        this.name = (id == 0) ? "Border" : String.format("Pattern %d", id);
    }

    /**
     * Determines pattern type based on ID.
     *
     * @param id Pattern ID
     * @return "border" for 0-5, "inner" for 6-22
     */
    private String determineType(int id) {
        if (id >= 0 && id <= 5) {
            return "border";
        } else if (id >= 6 && id <= 22) {
            return "inner";
        } else {
            return "unknown";
        }
    }

    /**
     * Validates that a pattern ID is in valid range (0-22).
     *
     * @param id Pattern ID to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidPatternId(int id) {
        return id >= 0 && id <= 22;
    }

    // Getters and Setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
        this.url = (id == 0) ? null : String.format("/patterns/pattern%d.png", id);
        this.type = determineType(id);
        this.name = (id == 0) ? "Border" : String.format("Pattern %d", id);
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "PatternInfo{" +
                "id=" + id +
                ", url='" + url + '\'' +
                ", type='" + type + '\'' +
                ", name='" + name + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PatternInfo that = (PatternInfo) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
