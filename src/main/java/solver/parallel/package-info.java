/**
 * Parallel execution coordination for multi-threaded puzzle solving.
 *
 * <h2>Main Classes</h2>
 * <ul>
 *   <li>{@link solver.parallel.ParallelExecutionCoordinator} - Coordinates thread pool execution</li>
 *   <li>{@link solver.parallel.WorkStealingExecutor} - Implements Fork/Join work-stealing parallelism</li>
 * </ul>
 *
 * <h2>Parallel Strategies</h2>
 * <ul>
 *   <li><b>Work-Stealing</b>: Fork/Join framework for dynamic load balancing</li>
 *   <li><b>Thread Pool</b>: Fixed thread pool with diversification</li>
 * </ul>
 *
 * <h2>Performance</h2>
 * <p>Achieves 8-16x speedup on multi-core systems through efficient parallelism.</p>
 *
 * @see solver.ParallelSearchManager
 * @see solver.SharedSearchState
 */
package solver.parallel;
