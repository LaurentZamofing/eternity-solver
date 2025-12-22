/**
 * Unified board visualization and display services.
 *
 * <h2>Main Classes</h2>
 * <ul>
 *   <li>{@link solver.display.BoardDisplayService} - Unified facade for board visualization (Phase 2.3)</li>
 *   <li>{@link solver.display.AbstractBoardRenderer} - Base class for renderers</li>
 *   <li>{@link solver.display.LabeledBoardRenderer} - Renders board with color-coded edges</li>
 *   <li>{@link solver.display.ComparisonBoardRenderer} - Renders side-by-side board comparison</li>
 *   <li>{@link solver.display.EdgeMatchingValidator} - Validates edge matching</li>
 *   <li>{@link solver.display.ValidPieceCounter} - Counts correctly placed pieces</li>
 * </ul>
 *
 * <h2>Design</h2>
 * <p>Consolidates fragmented visualization code (Phase 5 refactoring):</p>
 * <ul>
 *   <li>Replaces deprecated util/BoardRenderer, BoardTextRenderer, SaveBoardRenderer</li>
 *   <li>Provides consistent API for all visualization needs</li>
 *   <li>Supports console output, save files, and monitoring dashboard</li>
 * </ul>
 *
 * @see solver.BoardDisplayManager
 * @see solver.BoardVisualizer
 */
package solver.display;
