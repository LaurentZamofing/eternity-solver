/**
 * Utility classes for puzzle solving, state management, and system operations.
 * <p>
 * This package contains helper classes and utilities used throughout the solver:
 * state persistence, logging, formatting, configuration, and system management.
 *
 * <h2>Main Components</h2>
 *
 * <h3>State Management</h3>
 * <ul>
 *   <li>{@link util.SaveStateManager} - Facade for save/load operations</li>
 *   <li>{@link util.state.SaveStateIO} - File I/O for puzzle state</li>
 *   <li>{@link util.state.SaveStateSerializer} - JSON serialization/deserialization</li>
 *   <li>{@link util.state.SaveFileManager} - File discovery and management</li>
 *   <li>{@link util.state.BinarySaveManager} - Binary format handling</li>
 * </ul>
 *
 * <h3>Logging and Output</h3>
 * <ul>
 *   <li>{@link util.SolverLogger} - Centralized logging facade</li>
 *   <li>{@link util.StatsLogger} - Statistics and metrics logging</li>
 *   <li>{@link util.FormattingUtils} - String formatting utilities</li>
 * </ul>
 *
 * <h3>Configuration and Constants</h3>
 * <ul>
 *   <li>{@link util.ConfigurationUtils} - Configuration file utilities</li>
 *   <li>{@link util.TimeConstants} - Time-related constants</li>
 * </ul>
 *
 * <h3>System Management</h3>
 * <ul>
 *   <li>{@link util.ShutdownManager} - Graceful shutdown handling</li>
 *   <li>{@link util.ExceptionHandler} - Centralized exception handling</li>
 * </ul>
 *
 * <h3>Rendering and Analysis</h3>
 * <ul>
 *   <li>{@link util.BoardRenderer} - Board visualization to files</li>
 *   <li>{@link util.ComparisonAnalyzer} - Board comparison analysis</li>
 * </ul>
 *
 * <h2>Save State Management</h2>
 * <p>
 * The state management system provides automatic checkpoint/resume functionality:
 *
 * <h3>Save File Types</h3>
 * <table border="1">
 *   <caption>Save File Naming Convention</caption>
 *   <tr>
 *     <th>Type</th>
 *     <th>Pattern</th>
 *     <th>Purpose</th>
 *   </tr>
 *   <tr>
 *     <td>Current</td>
 *     <td>current_*.txt</td>
 *     <td>Latest checkpoint for resume</td>
 *   </tr>
 *   <tr>
 *     <td>Best</td>
 *     <td>best_*.txt</td>
 *     <td>Best solution found so far</td>
 *   </tr>
 *   <tr>
 *     <td>Thread</td>
 *     <td>thread_*.txt</td>
 *     <td>Per-thread checkpoint (parallel solving)</td>
 *   </tr>
 *   <tr>
 *     <td>Milestone</td>
 *     <td>best_XXX_*.txt</td>
 *     <td>Milestone achievements (never overwritten)</td>
 *   </tr>
 * </table>
 *
 * <h3>Save State Format</h3>
 * <pre>
 * {
 *   "depth": 42,
 *   "placedPieces": [
 *     {"row": 0, "col": 0, "pieceId": 1, "rotation": 0},
 *     {"row": 0, "col": 1, "pieceId": 5, "rotation": 2},
 *     ...
 *   ],
 *   "unusedPieceIds": [3, 7, 11, ...],
 *   "placementOrder": [1, 5, 9, ...],
 *   "totalComputeTimeMs": 123456,
 *   "timestamp": "2025-12-10T15:30:00"
 * }
 * </pre>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * // Save state
 * SaveStateManager.saveCurrentState(board, pieces, depth, "eternity2_p01");
 *
 * // Load state
 * File saveFile = SaveStateManager.findCurrentSave("eternity2_p01");
 * if (saveFile != null) {
 *     SaveStateManager.SaveState state =
 *         SaveStateManager.loadStateFromFile(saveFile, "eternity2");
 *     SaveStateManager.restoreState(state, board, pieces);
 * }
 * }</pre>
 *
 * <h2>Logging System</h2>
 * <p>
 * Centralized logging through {@link util.SolverLogger}:
 *
 * <h3>Log Levels</h3>
 * <ul>
 *   <li><b>ERROR</b>: Critical errors requiring attention</li>
 *   <li><b>WARN</b>: Warnings about potential issues</li>
 *   <li><b>INFO</b>: General information (default)</li>
 *   <li><b>DEBUG</b>: Detailed debugging information</li>
 * </ul>
 *
 * <h3>Usage Pattern</h3>
 * <pre>{@code
 * // Instead of System.out.println:
 * SolverLogger.info("Placed piece " + pieceId + " at (" + row + "," + col + ")");
 *
 * // Error logging with exception:
 * SolverLogger.error("Failed to save state", exception);
 *
 * // Debug logging (only when verbose enabled):
 * SolverLogger.debug("Domain size after propagation: " + domainSize);
 * }</pre>
 *
 * <h2>Time Constants</h2>
 * <p>
 * {@link util.TimeConstants} provides standardized time units:
 * <pre>{@code
 * long fiveMinutes = 5 * TimeConstants.SECONDS_PER_MINUTE * TimeConstants.MILLIS_PER_SECOND;
 * long oneHour = TimeConstants.SECONDS_PER_HOUR * TimeConstants.MILLIS_PER_SECOND;
 * }</pre>
 *
 * <h2>Configuration Management</h2>
 * <p>
 * {@link util.ConfigurationUtils} provides utilities for puzzle configuration:
 * <ul>
 *   <li>Extract config ID from file path</li>
 *   <li>Create thread labels for parallel execution</li>
 *   <li>Parse configuration file names</li>
 * </ul>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * String configId = ConfigurationUtils.extractConfigId(
 *     "data/eternity2/eternity2_p01_ascending.txt"
 * );
 * // Result: "eternity2_p01_ascending"
 *
 * String label = ConfigurationUtils.createThreadLabel(3, configId);
 * // Result: "[T03:eternity2_p01_ascending]"
 * }</pre>
 *
 * <h2>Shutdown Management</h2>
 * <p>
 * {@link util.ShutdownManager} provides graceful shutdown:
 * <ul>
 *   <li>Registers shutdown hooks for clean exit</li>
 *   <li>Saves state before termination</li>
 *   <li>Closes resources properly</li>
 *   <li>Handles Ctrl+C interrupts</li>
 * </ul>
 *
 * <h2>Board Rendering</h2>
 * <p>
 * {@link util.BoardRenderer} creates visual representations:
 * <ul>
 *   <li>PNG images of board state</li>
 *   <li>Color-coded piece visualization</li>
 *   <li>Progress tracking over time</li>
 *   <li>Comparison between states</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <table border="1">
 *   <caption>Thread Safety of Utility Classes</caption>
 *   <tr>
 *     <th>Class</th>
 *     <th>Thread Safety</th>
 *     <th>Notes</th>
 *   </tr>
 *   <tr>
 *     <td>SaveStateManager</td>
 *     <td>Thread-safe</td>
 *     <td>Synchronized save operations</td>
 *   </tr>
 *   <tr>
 *     <td>SolverLogger</td>
 *     <td>Thread-safe</td>
 *     <td>Backed by SLF4J/Logback</td>
 *   </tr>
 *   <tr>
 *     <td>TimeConstants</td>
 *     <td>Thread-safe</td>
 *     <td>Immutable constants</td>
 *   </tr>
 *   <tr>
 *     <td>ConfigurationUtils</td>
 *     <td>Thread-safe</td>
 *     <td>Stateless utility methods</td>
 *   </tr>
 *   <tr>
 *     <td>ShutdownManager</td>
 *     <td>Thread-safe</td>
 *     <td>JVM shutdown hooks</td>
 *   </tr>
 * </table>
 *
 * <h2>Performance Considerations</h2>
 * <ul>
 *   <li><b>Save Operations</b>: Periodic saves (every 60s) to avoid I/O overhead</li>
 *   <li><b>Logging</b>: Debug logging has minimal overhead when disabled</li>
 *   <li><b>File Discovery</b>: Cached results for repeated lookups</li>
 *   <li><b>Binary Format</b>: Optional for faster save/load (not yet implemented)</li>
 * </ul>
 *
 * <h2>Error Handling</h2>
 * <p>
 * {@link util.ExceptionHandler} provides centralized error handling:
 * <ul>
 *   <li>Logs exceptions with context</li>
 *   <li>Attempts recovery when possible</li>
 *   <li>Prevents solver crashes from I/O errors</li>
 *   <li>Provides user-friendly error messages</li>
 * </ul>
 *
 * @see model Data model package
 * @see solver Solver algorithms package
 * @see util.state State management subpackage
 * @since 1.0
 * @author Eternity Solver Team
 */
package util;
