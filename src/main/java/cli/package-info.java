/**
 * Command-line interface (CLI) components for the Eternity puzzle solver.
 * <p>
 * This package provides argument parsing, validation, and help text generation
 * for the solver's command-line interface. It separates CLI concerns from the
 * core business logic.
 *
 * <h2>Main Components</h2>
 * <ul>
 *   <li>{@link cli.CommandLineInterface} - Argument parser and validator</li>
 * </ul>
 *
 * <h2>Supported Arguments</h2>
 *
 * <h3>Help and Information</h3>
 * <table border="1">
 *   <caption>Information Options</caption>
 *   <tr>
 *     <th>Option</th>
 *     <th>Alias</th>
 *     <th>Description</th>
 *   </tr>
 *   <tr>
 *     <td>--help</td>
 *     <td>-h</td>
 *     <td>Display help text and exit</td>
 *   </tr>
 *   <tr>
 *     <td>--version</td>
 *     <td></td>
 *     <td>Display version information</td>
 *   </tr>
 * </table>
 *
 * <h3>Execution Options</h3>
 * <table border="1">
 *   <caption>Solver Configuration</caption>
 *   <tr>
 *     <th>Option</th>
 *     <th>Alias</th>
 *     <th>Type</th>
 *     <th>Description</th>
 *   </tr>
 *   <tr>
 *     <td>--puzzle</td>
 *     <td></td>
 *     <td>string</td>
 *     <td>Puzzle name to solve</td>
 *   </tr>
 *   <tr>
 *     <td>--verbose</td>
 *     <td>-v</td>
 *     <td>flag</td>
 *     <td>Enable verbose logging</td>
 *   </tr>
 *   <tr>
 *     <td>--quiet</td>
 *     <td>-q</td>
 *     <td>flag</td>
 *     <td>Suppress non-essential output</td>
 *   </tr>
 *   <tr>
 *     <td>--parallel</td>
 *     <td>-p</td>
 *     <td>flag</td>
 *     <td>Enable parallel solving</td>
 *   </tr>
 *   <tr>
 *     <td>--threads</td>
 *     <td>-t</td>
 *     <td>integer</td>
 *     <td>Number of threads (default: auto)</td>
 *   </tr>
 *   <tr>
 *     <td>--timeout</td>
 *     <td></td>
 *     <td>integer</td>
 *     <td>Timeout in seconds</td>
 *   </tr>
 *   <tr>
 *     <td>--no-singletons</td>
 *     <td></td>
 *     <td>flag</td>
 *     <td>Disable singleton detection</td>
 *   </tr>
 * </table>
 *
 * <h2>Argument Formats</h2>
 *
 * <h3>Boolean Flags</h3>
 * <pre>
 * # Short form
 * java -jar solver.jar -v -p
 *
 * # Long form
 * java -jar solver.jar --verbose --parallel
 * </pre>
 *
 * <h3>Value Options</h3>
 * <pre>
 * # Space-separated
 * java -jar solver.jar --puzzle eternity2 --threads 8
 *
 * # Equals sign
 * java -jar solver.jar --puzzle=eternity2 --threads=8
 *
 * # Short form with space
 * java -jar solver.jar -t 8
 * </pre>
 *
 * <h3>Positional Arguments</h3>
 * <pre>
 * # Puzzle name as positional argument (first non-option argument)
 * java -jar solver.jar eternity2 --verbose
 * </pre>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Basic Usage</h3>
 * <pre>
 * # Solve example puzzle with verbose output
 * java -jar solver.jar --puzzle example_4x4 --verbose
 *
 * # Solve with parallel execution
 * java -jar solver.jar --puzzle indice1 --parallel --threads 4
 *
 * # Solve with timeout
 * java -jar solver.jar --puzzle eternity2 --timeout 3600
 * </pre>
 *
 * <h3>Advanced Usage</h3>
 * <pre>
 * # Parallel with custom configuration
 * java -jar solver.jar eternity2 -p -t 16 -v --timeout 7200
 *
 * # Quiet mode (minimal output)
 * java -jar solver.jar indice6 -q --no-singletons
 *
 * # Display help
 * java -jar solver.jar --help
 * </pre>
 *
 * <h2>Validation</h2>
 * <p>
 * {@link cli.CommandLineInterface} validates arguments:
 * <ul>
 *   <li>Required arguments present</li>
 *   <li>Numeric values are valid integers</li>
 *   <li>Thread count is positive</li>
 *   <li>Timeout value is positive</li>
 *   <li>Mutually exclusive options (verbose/quiet)</li>
 * </ul>
 *
 * <h3>Validation Examples</h3>
 * <pre>
 * # Invalid: negative thread count
 * java -jar solver.jar --threads -1
 * Error: Option --threads requires a positive integer
 *
 * # Invalid: missing required argument
 * java -jar solver.jar --puzzle
 * Error: Option --puzzle requires an argument
 *
 * # Invalid: both verbose and quiet
 * java -jar solver.jar -v -q
 * Error: Options --verbose and --quiet are mutually exclusive
 * </pre>
 *
 * <h2>Error Handling</h2>
 * <p>
 * The CLI provides user-friendly error messages:
 * <ul>
 *   <li>Unknown options → suggests similar valid options</li>
 *   <li>Invalid values → shows expected format</li>
 *   <li>Missing arguments → shows usage example</li>
 *   <li>Parse errors → displays help text</li>
 * </ul>
 *
 * <h2>Integration</h2>
 * <pre>
 * ┌──────────────────────────────────────┐
 * │      Main Entry Point                │
 * │  (MainCLI.main())                    │
 * └──────────────┬───────────────────────┘
 *                │
 * ┌──────────────▼───────────────────────┐
 * │  CommandLineInterface                │
 * │  - Parse arguments                   │
 * │  - Validate options                  │
 * │  - Generate help text                │
 * └──────────────┬───────────────────────┘
 *                │
 * ┌──────────────▼───────────────────────┐
 * │  PuzzleExecutor / PuzzleRunner       │
 * │  - Load puzzle                       │
 * │  - Configure solver                  │
 * │  - Execute solving                   │
 * └──────────────────────────────────────┘
 * </pre>
 *
 * <h2>Code Example</h2>
 * <pre>{@code
 * public static void main(String[] args) {
 *     // Parse arguments
 *     CommandLineInterface cli = new CommandLineInterface();
 *     if (!cli.parse(args)) {
 *         System.err.println("Error: " + cli.getErrorMessage());
 *         cli.printHelp();
 *         System.exit(1);
 *     }
 *
 *     // Show help if requested
 *     if (cli.isHelpRequested()) {
 *         cli.printHelp();
 *         System.exit(0);
 *     }
 *
 *     // Extract configuration
 *     String puzzleName = cli.getOption("puzzle", "example_4x4");
 *     boolean verbose = cli.hasOption("verbose");
 *     boolean parallel = cli.hasOption("parallel");
 *     int threads = cli.getIntOption("threads", -1);
 *     Integer timeout = cli.getIntOption("timeout", null);
 *
 *     // Execute with parsed configuration
 *     PuzzleRunner runner = new PuzzleRunner(board, pieces,
 *         new PuzzleRunner.PuzzleRunnerConfig()
 *             .setVerbose(verbose)
 *             .setParallel(parallel)
 *             .setThreads(threads)
 *             .setTimeoutSeconds(timeout)
 *     );
 *
 *     boolean solved = runner.run();
 *     System.exit(solved ? 0 : 1);
 * }
 * }</pre>
 *
 * <h2>Help Text</h2>
 * <p>
 * The CLI generates formatted help text:
 * <pre>
 * Eternity Puzzle Solver v1.0
 *
 * Usage:
 *   java -jar solver.jar [OPTIONS] [PUZZLE_NAME]
 *
 * Options:
 *   -h, --help              Show this help message
 *   --version               Show version information
 *   -v, --verbose           Enable verbose logging
 *   -q, --quiet             Suppress non-essential output
 *   -p, --parallel          Enable parallel solving
 *   -t, --threads <n>       Number of threads (default: auto)
 *   --timeout <seconds>     Timeout in seconds
 *   --puzzle <name>         Puzzle to solve
 *   --no-singletons         Disable singleton detection
 *
 * Examples:
 *   java -jar solver.jar --puzzle example_4x4
 *   java -jar solver.jar eternity2 -p -t 8 -v
 *   java -jar solver.jar indice1 --timeout 300
 *
 * Available Puzzles:
 *   example_3x3, example_4x4, indice1-6, eternity2
 * </pre>
 *
 * <h2>Exit Codes</h2>
 * <table border="1">
 *   <caption>Exit Status Codes</caption>
 *   <tr>
 *     <th>Code</th>
 *     <th>Meaning</th>
 *   </tr>
 *   <tr>
 *     <td>0</td>
 *     <td>Success - solution found</td>
 *   </tr>
 *   <tr>
 *     <td>1</td>
 *     <td>No solution found or parsing error</td>
 *   </tr>
 *   <tr>
 *     <td>2</td>
 *     <td>Timeout reached</td>
 *   </tr>
 *   <tr>
 *     <td>130</td>
 *     <td>Interrupted (Ctrl+C)</td>
 *   </tr>
 * </table>
 *
 * <h2>Thread Safety</h2>
 * <ul>
 *   <li>{@link cli.CommandLineInterface} - Not thread-safe (designed for single-threaded parsing)</li>
 * </ul>
 *
 * @see runner.PuzzleRunner Runner configuration
 * @see runner.PuzzleExecutor Main execution flow
 * @since 1.0
 * @author Eternity Solver Team
 */
package cli;
