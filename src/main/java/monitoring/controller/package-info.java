/**
 * REST and WebSocket controllers for monitoring dashboard.
 *
 * <h2>Main Controllers</h2>
 * <ul>
 *   <li>{@link monitoring.controller.DashboardController} - Main dashboard REST API</li>
 *   <li>{@link monitoring.controller.PatternController} - Pattern image serving</li>
 *   <li>{@link monitoring.controller.MetricsWebSocketController} - Real-time metrics via WebSocket</li>
 * </ul>
 *
 * <h2>Endpoints</h2>
 * <ul>
 *   <li>GET /api/metrics - Current solver metrics</li>
 *   <li>GET /api/board-state - Current board state</li>
 *   <li>GET /patterns/{id}.png - Pattern images</li>
 *   <li>WebSocket /ws/metrics - Real-time updates</li>
 * </ul>
 *
 * @see monitoring.service
 * @see monitoring.model
 */
package monitoring.controller;
