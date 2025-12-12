/**
 * Puzzle execution orchestration and runner framework.
 * <p>
 * This package provides the high-level execution framework for running puzzle
 * solving tasks. It separates business logic from CLI interfaces and provides
 * a clean abstraction for puzzle configuration and execution.
 *
 * <h2>Main Components</h2>
 *
 * <h3>Puzzle Definitions</h3>
 * <ul>
 *   <li>{@link runner.PuzzleDefinition} - Enum of all available puzzles with metadata</li>
 *   <li>{@link runner.PuzzleRunner} - Orchestrator for puzzle execution</li>
 *   <li>{@link runner.PuzzleExecutor} - Main execution flow and CLI integration</li>
 * </ul>
 *
 * <h2>Features</h2>
 *
 * <h3>Puzzle Catalog</h3>
 * <p>
 * {@link runner.PuzzleDefinition} centralizes all puzzle definitions that were
 * previously scattered throughout the codebase:
 * <ul>
 *   <li>Example puzzles: 3x3, 4x4 (easy/hard/ordered), 5x5</li>
 *   <li>Test puzzles: P01, P02, P03, P04</li>
 *   <li>Indice puzzles: Indice 1, 2, 3, 4, 5, 6</li>
 *   <li>Eternity II: Full 16x16 puzzle (256 pieces)</li>
 * </ul>
 *
 * <p>Each puzzle definition includes:
 * <ul>
 *   <li>Display name and dimensions</li>
 *   <li>Piece loading logic</li>
 *   <li>Solver configuration defaults</li>
 *   <li>Optional hint/fixed piece placements</li>
 * </ul>
 *
 * <h3>Execution Configuration</h3>
 * <p>
 * {@link runner.PuzzleRunner.PuzzleRunnerConfig} provides builder-style
 * configuration for puzzle execution:
 * <ul>
 *   <li><b>verbose</b> - Enable detailed logging</li>
 *   <li><b>parallel</b> - Use parallel solving</li>
 *   <li><b>useSingletons</b> - Enable singleton detection</li>
 *   <li><b>threads</b> - Number of threads (-1 = auto)</li>
 *   <li><b>minDepth</b> - Minimum depth for parallelization</li>
 *   <li><b>timeoutSeconds</b> - Optional timeout</li>
 * </ul>
 *
 * <h3>Execution Flow</h3>
 * <pre>
 * ┌──────────────────────────────────────────┐
 * │      PuzzleExecutor (CLI Entry)          │
 * │  - Parses command line arguments         │
 * │  - Selects puzzle definition             │
 * │  - Creates configuration                 │
 * └──────────────┬───────────────────────────┘
 *                │
 * ┌──────────────▼───────────────────────────┐
 * │      PuzzleRunner (Orchestrator)         │
 * │  - Initializes board and pieces          │
 * │  - Configures solver                     │
 * │  - Executes solving                      │
 * │  - Handles shutdown/timeout              │
 * └──────────────┬───────────────────────────┘
 *                │
 * ┌──────────────▼───────────────────────────┐
 * │      EternitySolver (Core Logic)         │
 * │  - AC-3 constraint propagation           │
 * │  - MRV heuristic                         │
 * │  - Backtracking search                   │
 * └──────────────────────────────────────────┘
 * </pre>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create configuration
 * PuzzleRunner.PuzzleRunnerConfig config = new PuzzleRunner.PuzzleRunnerConfig()
 *     .setVerbose(true)
 *     .setParallel(true)
 *     .setThreads(4)
 *     .setTimeoutSeconds(300);
 *
 * // Load puzzle
 * PuzzleDefinition puzzle = PuzzleDefinition.EXAMPLE_4X4_HARD;
 * Map<Integer, Piece> pieces = puzzle.loadPieces();
 * Board board = new Board(puzzle.rows, puzzle.cols);
 *
 * // Execute
 * PuzzleRunner runner = new PuzzleRunner(board, pieces, config);
 * boolean solved = runner.run();
 *
 * if (solved) {
 *     System.out.println("Solution found!");
 *     runner.printSolution();
 * }
 * }</pre>
 *
 * <h2>Puzzle Selection</h2>
 * <table border="1">
 *   <caption>Available Puzzles</caption>
 *   <tr>
 *     <th>Category</th>
 *     <th>Puzzle</th>
 *     <th>Size</th>
 *     <th>Difficulty</th>
 *   </tr>
 *   <tr>
 *     <td rowspan="5">Examples</td>
 *     <td>EXAMPLE_3X3</td>
 *     <td>3×3 (9 pieces)</td>
 *     <td>Trivial</td>
 *   </tr>
 *   <tr>
 *     <td>EXAMPLE_4X4_EASY</td>
 *     <td>4×4 (16 pieces)</td>
 *     <td>Easy</td>
 *   </tr>
 *   <tr>
 *     <td>EXAMPLE_4X4_HARD</td>
 *     <td>4×4 (16 pieces)</td>
 *     <td>Hard</td>
 *   </tr>
 *   <tr>
 *     <td>EXAMPLE_4X4_ORDERED</td>
 *     <td>4×4 (16 pieces)</td>
 *     <td>Trivial (ordered)</td>
 *   </tr>
 *   <tr>
 *     <td>EXAMPLE_5X5</td>
 *     <td>5×5 (25 pieces)</td>
 *     <td>Medium</td>
 *   </tr>
 *   <tr>
 *     <td rowspan="4">Test</td>
 *     <td>TEST_PUZZLE_P01</td>
 *     <td>4×4 (16 pieces)</td>
 *     <td>Test case</td>
 *   </tr>
 *   <tr>
 *     <td>TEST_PUZZLE_P02</td>
 *     <td>4×4 (16 pieces)</td>
 *     <td>Test case</td>
 *   </tr>
 *   <tr>
 *     <td>TEST_PUZZLE_P03</td>
 *     <td>4×4 (16 pieces)</td>
 *     <td>Test case</td>
 *   </tr>
 *   <tr>
 *     <td>TEST_PUZZLE_P04</td>
 *     <td>4×4 (16 pieces)</td>
 *     <td>Test case</td>
 *   </tr>
 *   <tr>
 *     <td rowspan="6">Indice</td>
 *     <td>INDICE1</td>
 *     <td>4×4 (16 pieces)</td>
 *     <td>Progressive</td>
 *   </tr>
 *   <tr>
 *     <td>INDICE2</td>
 *     <td>4×4 (16 pieces)</td>
 *     <td>Progressive</td>
 *   </tr>
 *   <tr>
 *     <td>INDICE3</td>
 *     <td>4×4 (16 pieces)</td>
 *     <td>Progressive</td>
 *   </tr>
 *   <tr>
 *     <td>INDICE4</td>
 *     <td>4×4 (16 pieces)</td>
 *     <td>Progressive</td>
 *   </tr>
 *   <tr>
 *     <td>INDICE5</td>
 *     <td>4×4 (16 pieces)</td>
 *     <td>Progressive</td>
 *   </tr>
 *   <tr>
 *     <td>INDICE6</td>
 *     <td>4×4 (16 pieces)</td>
 *     <td>Progressive</td>
 *   </tr>
 *   <tr>
 *     <td>Challenge</td>
 *     <td>ETERNITY_II</td>
 *     <td>16×16 (256 pieces)</td>
 *     <td>Unsolved ($2M prize)</td>
 *   </tr>
 * </table>
 *
 * <h2>Configuration Recommendations</h2>
 *
 * <h3>Small Puzzles (≤ 25 pieces)</h3>
 * <ul>
 *   <li>parallel = false (sequential is faster)</li>
 *   <li>verbose = true (to see progress)</li>
 *   <li>useSingletons = true (reduces search space)</li>
 * </ul>
 *
 * <h3>Medium Puzzles (25-100 pieces)</h3>
 * <ul>
 *   <li>parallel = true</li>
 *   <li>threads = 4-8 (based on CPU cores)</li>
 *   <li>minDepth = 5-10 (parallelization depth)</li>
 *   <li>timeoutSeconds = 300-600</li>
 * </ul>
 *
 * <h3>Large Puzzles (> 100 pieces)</h3>
 * <ul>
 *   <li>parallel = true</li>
 *   <li>threads = 8-16 (maximize CPU usage)</li>
 *   <li>minDepth = 10-15</li>
 *   <li>timeoutSeconds = 3600+ (hours/days)</li>
 *   <li>Enable checkpoint/resume via save files</li>
 * </ul>
 *
 * <h2>Graceful Shutdown</h2>
 * <p>
 * {@link runner.PuzzleRunner} integrates with {@link util.ShutdownManager}
 * to provide graceful shutdown:
 * <ul>
 *   <li>Ctrl+C handling - saves state before exit</li>
 *   <li>Timeout handling - returns partial solution</li>
 *   <li>Exception handling - cleans up resources</li>
 *   <li>Statistics reporting - displays metrics on exit</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <ul>
 *   <li>{@link runner.PuzzleDefinition} - Thread-safe (enum)</li>
 *   <li>{@link runner.PuzzleRunner} - Not thread-safe (single-threaded orchestrator)</li>
 *   <li>{@link runner.PuzzleExecutor} - Not thread-safe (main entry point)</li>
 * </ul>
 *
 * <h2>Performance</h2>
 * <p>
 * Execution performance varies by puzzle:
 * <ul>
 *   <li>3x3 Example: < 1 second</li>
 *   <li>4x4 Easy: 1-5 seconds</li>
 *   <li>4x4 Hard: 5-60 seconds</li>
 *   <li>5x5: 1-10 minutes</li>
 *   <li>Indice 1-6: Minutes to hours</li>
 *   <li>Eternity II: Unsolved (exponential complexity)</li>
 * </ul>
 *
 * @see solver.EternitySolver Core solving logic
 * @see model.Board Board representation
 * @see util.ShutdownManager Graceful shutdown
 * @since 1.0
 * @author Eternity Solver Team
 */
package runner;
