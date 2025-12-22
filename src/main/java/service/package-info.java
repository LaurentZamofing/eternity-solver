/**
 * High-level services for puzzle solving orchestration.
 *
 * <h2>Main Classes</h2>
 * <ul>
 *   <li>{@link service.PuzzleSolverOrchestrator} - Orchestrates puzzle solving with timeout and result handling</li>
 *   <li>{@link service.SaveStateRestorationService} - Handles save state restoration logic</li>
 *   <li>{@link service.SolutionDisplayService} - Manages solution display and output</li>
 *   <li>{@link service.TimeoutExecutor} - Executes puzzle solving with configurable timeout</li>
 * </ul>
 *
 * <h2>Design Pattern</h2>
 * <p>Service layer pattern - coordinates between CLI/UI layer and solver core logic.</p>
 *
 * @see solver.EternitySolver
 * @see runner.PuzzleRunner
 */
package service;
