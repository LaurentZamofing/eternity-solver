/**
 * Output strategies for placement logging and visualization.
 *
 * <h2>Purpose</h2>
 * This package implements the <b>Strategy pattern</b> to separate logging/UI concerns
 * from core solver algorithm logic. This allows the same solver code to run in:
 * <ul>
 *   <li><b>Verbose mode</b> - Detailed step-by-step debugging output</li>
 *   <li><b>Quiet mode</b> - Silent execution for production/batch runs</li>
 *   <li><b>Custom modes</b> - Future implementations (JSON logging, GUI, etc.)</li>
 * </ul>
 *
 * <h2>Design Pattern</h2>
 * <pre>
 * ┌─────────────────────────────┐
 * │ PlacementOutputStrategy     │◄─── Interface
 * │ (interface)                 │
 * ├─────────────────────────────┤
 * │ + logCellSelection()        │
 * │ + logPlacementAttempt()     │
 * │ + logBacktrack()            │
 * │ + waitForUser()             │
 * └─────────────────────────────┘
 *         ▲          ▲
 *         │          │
 *         │          │
 * ┌───────┴──┐  ┌───┴────────┐
 * │ Verbose  │  │   Quiet    │
 * │ Output   │  │   Output   │
 * └──────────┘  └────────────┘
 * </pre>
 *
 * <h2>Key Classes</h2>
 * <table border="1">
 *   <tr>
 *     <th>Class</th>
 *     <th>Purpose</th>
 *     <th>When to Use</th>
 *   </tr>
 *   <tr>
 *     <td>{@link solver.output.PlacementOutputStrategy}</td>
 *     <td>Strategy interface defining output operations</td>
 *     <td>Implement for custom output behavior</td>
 *   </tr>
 *   <tr>
 *     <td>{@link solver.output.VerbosePlacementOutput}</td>
 *     <td>Detailed console output with user interaction</td>
 *     <td>Debugging, visualization, step-through mode</td>
 *   </tr>
 *   <tr>
 *     <td>{@link solver.output.QuietPlacementOutput}</td>
 *     <td>No-op implementation (silent)</td>
 *     <td>Production runs, automated testing, parallel execution</td>
 *   </tr>
 * </table>
 *
 * <h2>Usage Example</h2>
 * <pre>
 * // Verbose mode with detailed logging
 * PlacementOutputStrategy output = new VerbosePlacementOutput();
 * MRVPlacementStrategy strategy = new MRVPlacementStrategy(
 *     true,  // verbose = true
 *     valueOrderer,
 *     symmetryManager,
 *     propagator,
 *     domainManager
 * );
 *
 * // Quiet mode for production
 * PlacementOutputStrategy output = new QuietPlacementOutput();
 * MRVPlacementStrategy strategy = new MRVPlacementStrategy(
 *     false,  // verbose = false
 *     valueOrderer,
 *     symmetryManager,
 *     propagator,
 *     domainManager
 * );
 * </pre>
 *
 * <h2>Benefits</h2>
 * <ul>
 *   <li><b>Separation of concerns</b> - Algorithm logic independent of logging</li>
 *   <li><b>Testability</b> - Easy to test algorithm without UI</li>
 *   <li><b>Performance</b> - Zero overhead in quiet mode</li>
 *   <li><b>Extensibility</b> - Add new output modes without changing solver</li>
 *   <li><b>Flexibility</b> - Runtime switching between verbose/quiet modes</li>
 * </ul>
 *
 * <h2>Future Extensions</h2>
 * Potential future implementations:
 * <ul>
 *   <li><b>JsonPlacementOutput</b> - Structured logging for analysis</li>
 *   <li><b>GuiPlacementOutput</b> - Real-time visualization in GUI</li>
 *   <li><b>MetricsPlacementOutput</b> - Performance metrics collection</li>
 *   <li><b>RemotePlacementOutput</b> - WebSocket streaming to dashboard</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * Current implementations are <b>not thread-safe</b>. If sharing strategies across threads,
 * use separate instances per thread or implement thread-safe versions.
 *
 * @since 1.0.0
 * @author Eternity Solver Team
 * @see solver.MRVPlacementStrategy
 * @see solver.SingletonPlacementStrategy
 */
package solver.output;
