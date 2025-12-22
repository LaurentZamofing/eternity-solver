/**
 * Application entry points for the Eternity Puzzle Solver.
 *
 * <h2>Main Classes</h2>
 * <ul>
 *   <li>{@link app.Main} - Simple entry point with hardcoded puzzle selection</li>
 *   <li>{@link app.MainCLI} - Professional CLI with argument parsing</li>
 *   <li>{@link app.MainParallel} - Parallel launcher with thread rotation and priority scheduling</li>
 *   <li>{@link app.MainSequential} - Sequential launcher for all configurations</li>
 *   <li>{@link app.TestP02} - Quick test for specific p02 configuration</li>
 * </ul>
 *
 * <h2>Package Organization (Phase 1 Refactoring)</h2>
 * <p>Entry points were moved from root package to {@code app/} for better organization.</p>
 *
 * @see runner.PuzzleExecutor
 * @see cli.CommandLineInterface
 */
package app;
