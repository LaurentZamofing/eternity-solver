/**
 * Strategy pattern implementation for different solving approaches.
 *
 * <h2>Main Classes</h2>
 * <ul>
 *   <li>{@link solver.strategy.SolverStrategy} - Strategy interface</li>
 *   <li>{@link solver.strategy.SequentialStrategy} - Sequential solving strategy</li>
 *   <li>{@link solver.strategy.ParallelStrategy} - Parallel work-stealing strategy</li>
 *   <li>{@link solver.strategy.HistoricalStrategy} - Historical save restoration strategy</li>
 *   <li>{@link solver.strategy.SolverContext} - Execution context for strategies</li>
 *   <li>{@link solver.strategy.StrategyFactory} - Factory for creating strategies</li>
 * </ul>
 *
 * <h2>Design Pattern</h2>
 * <p>Strategy pattern allows switching between solving approaches dynamically.</p>
 *
 * <h2>Refactoring (Phase 1)</h2>
 * <p>Extracted from monolithic solver to enable flexible solving approaches.</p>
 *
 * @see solver.EternitySolver
 */
package solver.strategy;
