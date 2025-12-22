/**
 * Heuristic strategies for intelligent piece and cell selection.
 *
 * <h2>Main Classes</h2>
 * <ul>
 *   <li>{@link solver.heuristics.HeuristicStrategy} - Base interface for heuristics</li>
 *   <li>{@link solver.heuristics.MRVCellSelector} - Minimum Remaining Values cell selection (3-5x speedup)</li>
 *   <li>{@link solver.heuristics.LeastConstrainingValueOrderer} - Least constraining value ordering</li>
 * </ul>
 *
 * <h2>Performance Impact</h2>
 * <ul>
 *   <li><b>MRV (Minimum Remaining Values)</b>: 3-5x speedup by selecting most constrained cells first</li>
 *   <li><b>LCV (Least Constraining Value)</b>: Improves backtracking by trying least constraining pieces first</li>
 * </ul>
 *
 * <h2>Algorithm</h2>
 * <p>MRV: Select cell with fewest valid pieces remaining (most constrained).</p>
 * <p>LCV: Order pieces by how many options they leave for neighbors.</p>
 *
 * @see solver.EternitySolver
 * @see solver.DomainManager
 */
package solver.heuristics;
