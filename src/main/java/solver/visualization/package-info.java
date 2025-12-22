/**
 * Low-level board formatters with ANSI colors and grid drawing.
 *
 * <h2>Main Classes</h2>
 * <ul>
 *   <li>{@link solver.visualization.LabeledBoardFormatter} - Full board with labels and colors</li>
 *   <li>{@link solver.visualization.CompactBoardFormatter} - Compact representation</li>
 *   <li>{@link solver.visualization.DetailedBoardFormatter} - Detailed with edge values</li>
 *   <li>{@link solver.visualization.ComparisonBoardFormatter} - Side-by-side comparison</li>
 *   <li>{@link solver.visualization.AnsiColorHelper} - ANSI color constants</li>
 *   <li>{@link solver.visualization.GridDrawingHelper} - Grid drawing characters (Unicode box-drawing)</li>
 * </ul>
 *
 * <h2>Design</h2>
 * <p>Low-level formatters used by {@link solver.display.BoardDisplayService} and
 * {@link solver.BoardDisplayManager}.</p>
 *
 * <p>Uses static imports for color constants and grid characters for cleaner code.</p>
 *
 * @see solver.display.BoardDisplayService
 */
package solver.visualization;
