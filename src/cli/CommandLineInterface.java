package cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Command line interface for the Eternity solver.
 * Parses and validates arguments passed to the program.
 */
public class CommandLineInterface {

    private static final Logger logger = LoggerFactory.getLogger(CommandLineInterface.class);

    private final Map<String, String> options = new HashMap<>();
    private final List<String> arguments = new ArrayList<>();
    private boolean helpRequested = false;
    private String errorMessage = null;

    /**
     * Parses command line arguments.
     *
     * Supported formats:
     * - Short options: -v, -h
     * - Long options: --verbose, --help
     * - Options with value: --puzzle <name>, --threads <n>, --timeout <seconds>
     * - Positional arguments: puzzle_name
     *
     * @param args arguments from main()
     * @return true if parsing succeeded, false otherwise
     */
    public boolean parse(String[] args) {
        try {
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];

                if (arg.equals("--help") || arg.equals("-h")) {
                    helpRequested = true;
                    return true;
                }
                else if (arg.equals("--version")) {
                    options.put("version", "true");
                    return true;
                }
                else if (arg.equals("--verbose") || arg.equals("-v")) {
                    options.put("verbose", "true");
                }
                else if (arg.equals("--quiet") || arg.equals("-q")) {
                    options.put("quiet", "true");
                }
                else if (arg.equals("--parallel") || arg.equals("-p")) {
                    options.put("parallel", "true");
                }
                else if (arg.equals("--no-singletons")) {
                    options.put("singletons", "false");
                }
                else if (arg.startsWith("--puzzle=")) {
                    options.put("puzzle", arg.substring("--puzzle=".length()));
                }
                else if (arg.equals("--puzzle")) {
                    if (i + 1 >= args.length) {
                        errorMessage = "Option --puzzle requires an argument";
                        return false;
                    }
                    options.put("puzzle", args[++i]);
                }
                else if (arg.startsWith("--threads=")) {
                    String value = arg.substring("--threads=".length());
                    if (!validatePositiveInteger(value)) {
                        errorMessage = "Option --threads requires a positive integer: " + value;
                        return false;
                    }
                    options.put("threads", value);
                }
                else if (arg.equals("--threads") || arg.equals("-t")) {
                    if (i + 1 >= args.length) {
                        errorMessage = "Option --threads requires an argument";
                        return false;
                    }
                    String value = args[++i];
                    if (!validatePositiveInteger(value)) {
                        errorMessage = "Option --threads requires a positive integer: " + value;
                        return false;
                    }
                    options.put("threads", value);
                }
                else if (arg.startsWith("--timeout=")) {
                    String value = arg.substring("--timeout=".length());
                    if (!validatePositiveInteger(value)) {
                        errorMessage = "Option --timeout requires a positive integer: " + value;
                        return false;
                    }
                    options.put("timeout", value);
                }
                else if (arg.equals("--timeout")) {
                    if (i + 1 >= args.length) {
                        errorMessage = "Option --timeout requires an argument";
                        return false;
                    }
                    String value = args[++i];
                    if (!validatePositiveInteger(value)) {
                        errorMessage = "Option --timeout requires a positive integer: " + value;
                        return false;
                    }
                    options.put("timeout", value);
                }
                else if (arg.startsWith("--min-depth=")) {
                    String value = arg.substring("--min-depth=".length());
                    if (!validateNonNegativeInteger(value)) {
                        errorMessage = "Option --min-depth requires a non-negative integer: " + value;
                        return false;
                    }
                    options.put("min-depth", value);
                }
                else if (arg.equals("--min-depth")) {
                    if (i + 1 >= args.length) {
                        errorMessage = "Option --min-depth requires an argument";
                        return false;
                    }
                    String value = args[++i];
                    if (!validateNonNegativeInteger(value)) {
                        errorMessage = "Option --min-depth requires a non-negative integer: " + value;
                        return false;
                    }
                    options.put("min-depth", value);
                }
                else if (arg.startsWith("--")) {
                    errorMessage = "Unknown option: " + arg;
                    return false;
                }
                else if (arg.startsWith("-") && arg.length() > 1 && !arg.equals("-")) {
                    errorMessage = "Unknown short option: " + arg;
                    return false;
                }
                else {
                    // Positional argument (puzzle name)
                    arguments.add(arg);
                }
            }

            return true;

        } catch (Exception e) {
            errorMessage = "Parsing error: " + e.getMessage();
            logger.error("CLI parsing error", e);
            return false;
        }
    }

    /**
     * Validates that a string is a positive integer (> 0)
     */
    private boolean validatePositiveInteger(String value) {
        try {
            int n = Integer.parseInt(value);
            return n > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Validates that a string is a non-negative integer (>= 0)
     */
    private boolean validateNonNegativeInteger(String value) {
        try {
            int n = Integer.parseInt(value);
            return n >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Displays the help message
     */
    public void printHelp() {
        System.out.println("╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║           ETERNITY PUZZLE SOLVER - HELP                   ║");
        System.out.println("╚═══════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("USAGE:");
        System.out.println("  java -jar eternity-solver.jar [OPTIONS] [PUZZLE_NAME]");
        System.out.println();
        System.out.println("OPTIONS:");
        System.out.println("  -h, --help              Display this help message");
        System.out.println("  --version               Display program version");
        System.out.println("  -v, --verbose           Enable verbose mode (details)");
        System.out.println("  -q, --quiet             Quiet mode (errors only)");
        System.out.println("  -p, --parallel          Enable parallel search");
        System.out.println("  --no-singletons         Disable singleton optimization");
        System.out.println();
        System.out.println("  --puzzle <name>         Puzzle name to solve");
        System.out.println("  -t, --threads <n>       Number of threads (default: auto)");
        System.out.println("  --timeout <seconds>     Timeout in seconds (default: unlimited)");
        System.out.println("  --min-depth <n>         Minimum depth to display records");
        System.out.println();
        System.out.println("EXAMPLES:");
        System.out.println("  # Solve 6x12 puzzle in verbose mode");
        System.out.println("  java -jar eternity-solver.jar --puzzle puzzle_6x12 --verbose");
        System.out.println();
        System.out.println("  # Parallel search with 8 threads");
        System.out.println("  java -jar eternity-solver.jar -p --threads 8 eternity2_p1");
        System.out.println();
        System.out.println("  # Quiet mode with 1 hour timeout");
        System.out.println("  java -jar eternity-solver.jar -q --timeout 3600 puzzle_16x16");
        System.out.println();
        System.out.println("AVAILABLE PUZZLES:");
        System.out.println("  - puzzle_6x12       Puzzle 6×12 (72 pieces)");
        System.out.println("  - puzzle_16x16      Puzzle 16×16 (256 pieces)");
        System.out.println("  - validation_6x6    Test puzzle 6×6 (36 pieces)");
        System.out.println("  - eternity2_p*      Eternity II configurations");
        System.out.println("  - example_3x3       Simple example 3×3 (9 pieces)");
        System.out.println("  - example_4x4       Example 4×4 (16 pieces)");
        System.out.println();
    }

    /**
     * Displays the program version
     */
    public void printVersion() {
        System.out.println("Eternity Puzzle Solver v1.0.0");
        System.out.println("Edge-matching puzzle solver with optimized backtracking");
        System.out.println();
        System.out.println("Features:");
        System.out.println("  - MRV heuristic (Minimum Remaining Values)");
        System.out.println("  - Singleton detection (forced moves)");
        System.out.println("  - AC-3 constraint propagation");
        System.out.println("  - Multi-threaded parallel search");
        System.out.println("  - Automatic save/resume");
        System.out.println();
    }

    // ===== Getters =====

    public boolean isHelpRequested() {
        return helpRequested;
    }

    public boolean hasError() {
        return errorMessage != null;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getPuzzleName() {
        // Priority to --puzzle option
        if (options.containsKey("puzzle")) {
            return options.get("puzzle");
        }
        // Otherwise first positional argument
        if (!arguments.isEmpty()) {
            return arguments.get(0);
        }
        return null;
    }

    public boolean isVerbose() {
        return options.containsKey("verbose") && "true".equals(options.get("verbose"));
    }

    public boolean isQuiet() {
        return options.containsKey("quiet") && "true".equals(options.get("quiet"));
    }

    public boolean isParallel() {
        return options.containsKey("parallel") && "true".equals(options.get("parallel"));
    }

    public boolean useSingletons() {
        // Default true, false if --no-singletons
        return !"false".equals(options.get("singletons"));
    }

    public Integer getThreads() {
        if (options.containsKey("threads")) {
            try {
                return Integer.parseInt(options.get("threads"));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    public Integer getTimeout() {
        if (options.containsKey("timeout")) {
            try {
                return Integer.parseInt(options.get("timeout"));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    public Integer getMinDepth() {
        if (options.containsKey("min-depth")) {
            try {
                return Integer.parseInt(options.get("min-depth"));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    public boolean isVersionRequested() {
        return options.containsKey("version") && "true".equals(options.get("version"));
    }

    /**
     * Displays a summary of the parsed configuration (for debug)
     */
    public void printConfiguration() {
        logger.info("CLI Configuration:");
        logger.info("  Puzzle: {}", getPuzzleName());
        logger.info("  Verbose: {}", isVerbose());
        logger.info("  Quiet: {}", isQuiet());
        logger.info("  Parallel: {}", isParallel());
        logger.info("  Singletons: {}", useSingletons());
        logger.info("  Threads: {}", getThreads());
        logger.info("  Timeout: {}s", getTimeout());
        logger.info("  Min depth: {}", getMinDepth());
    }
}
