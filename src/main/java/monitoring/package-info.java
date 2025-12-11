/**
 * Real-time monitoring and web dashboard for puzzle solver progress.
 * <p>
 * This package provides a Spring Boot web application for monitoring multiple
 * puzzle solver instances in real-time through a browser-based dashboard.
 *
 * <h2>Architecture</h2>
 * <pre>
 * ┌─────────────────────────────────────────┐
 * │      Frontend (React + TypeScript)      │
 * │         http://localhost:3000            │
 * └──────────────┬──────────────────────────┘
 *                │ HTTP/WebSocket
 * ┌──────────────▼──────────────────────────┐
 * │   Spring Boot Backend (Port 8080)       │
 * │  ┌──────────────────────────────────┐   │
 * │  │  Controllers (REST + WebSocket)  │   │
 * │  └──────────────┬───────────────────┘   │
 * │  ┌──────────────▼───────────────────┐   │
 * │  │  Services (Business Logic)       │   │
 * │  └──────────────┬───────────────────┘   │
 * │  ┌──────────────▼───────────────────┐   │
 * │  │  Repository (JPA/H2 Database)    │   │
 * │  └──────────────────────────────────┘   │
 * └──────────────┬──────────────────────────┘
 *                │ File Watching
 * ┌──────────────▼──────────────────────────┐
 * │     saves/ directory (Solver Output)     │
 * │  current_*.txt, best_*.txt, etc.         │
 * └──────────────────────────────────────────┘
 * </pre>
 *
 * <h2>Main Components</h2>
 *
 * <h3>Application Entry Point</h3>
 * <ul>
 *   <li>{@link monitoring.MonitoringApplication} - Spring Boot main class</li>
 *   <li>{@link monitoring.MonitoringConstants} - Configuration constants</li>
 * </ul>
 *
 * <h3>Controllers (REST + WebSocket)</h3>
 * <ul>
 *   <li>{@link monitoring.controller.DashboardController} - Main REST API endpoints</li>
 *   <li>{@link monitoring.controller.PatternController} - Pattern image service</li>
 *   <li>{@link monitoring.controller.MetricsWebSocketController} - Real-time updates</li>
 * </ul>
 *
 * <h3>Services (Business Logic)</h3>
 * <ul>
 *   <li>{@link monitoring.service.IFileWatcherService} - File system monitoring</li>
 *   <li>{@link monitoring.service.SaveFileParser} - Parse save files</li>
 *   <li>{@link monitoring.service.MetricsAggregator} - Aggregate statistics</li>
 *   <li>{@link monitoring.service.CellDetailsService} - Board cell information</li>
 * </ul>
 *
 * <h3>Data Model</h3>
 * <ul>
 *   <li>{@link monitoring.model.ConfigMetrics} - Real-time configuration metrics</li>
 *   <li>{@link monitoring.model.HistoricalMetrics} - Database entity for history</li>
 *   <li>{@link monitoring.model.PatternInfo} - Pattern metadata</li>
 *   <li>{@link monitoring.model.CellInfo} - Board cell details</li>
 * </ul>
 *
 * <h3>Repository</h3>
 * <ul>
 *   <li>{@link monitoring.repository.MetricsRepository} - JPA repository for H2 database</li>
 * </ul>
 *
 * <h2>Features</h2>
 *
 * <h3>Real-Time Monitoring</h3>
 * <ul>
 *   <li>Live updates via WebSocket (every 2 seconds)</li>
 *   <li>Multiple solver configurations tracked simultaneously</li>
 *   <li>Progress percentage and depth tracking</li>
 *   <li>Performance metrics (pieces/second, compute time)</li>
 * </ul>
 *
 * <h3>Historical Data</h3>
 * <ul>
 *   <li>H2 database stores metrics history</li>
 *   <li>Time-series data for progress tracking</li>
 *   <li>Queryable by config name and time range</li>
 *   <li>Automatic persistence of all updates</li>
 * </ul>
 *
 * <h3>Board Visualization</h3>
 * <ul>
 *   <li>Interactive board grid with piece details</li>
 *   <li>Cell-by-cell information on hover</li>
 *   <li>Pattern images for visual identification</li>
 *   <li>Valid piece candidates for empty cells</li>
 * </ul>
 *
 * <h3>Pattern Service</h3>
 * <ul>
 *   <li>Serves pattern images (pattern1.png - pattern22.png)</li>
 *   <li>Pre-rendered piece images (001.png - 256.png)</li>
 *   <li>Pattern metadata API</li>
 *   <li>Health and statistics endpoints</li>
 * </ul>
 *
 * <h2>REST API Endpoints</h2>
 *
 * <h3>Configuration Monitoring</h3>
 * <table border="1">
 *   <caption>Main API Endpoints</caption>
 *   <tr>
 *     <th>Method</th>
 *     <th>Path</th>
 *     <th>Description</th>
 *   </tr>
 *   <tr>
 *     <td>GET</td>
 *     <td>/api/configs</td>
 *     <td>List all solver configurations</td>
 *   </tr>
 *   <tr>
 *     <td>GET</td>
 *     <td>/api/configs/{name}</td>
 *     <td>Get specific configuration metrics</td>
 *   </tr>
 *   <tr>
 *     <td>GET</td>
 *     <td>/api/configs/{name}/history</td>
 *     <td>Get historical metrics</td>
 *   </tr>
 *   <tr>
 *     <td>GET</td>
 *     <td>/api/configs/{name}/board</td>
 *     <td>Get board state with piece details</td>
 *   </tr>
 *   <tr>
 *     <td>GET</td>
 *     <td>/api/stats/global</td>
 *     <td>Global statistics across all configs</td>
 *   </tr>
 * </table>
 *
 * <h3>Pattern Service</h3>
 * <table border="1">
 *   <caption>Pattern API Endpoints</caption>
 *   <tr>
 *     <th>Method</th>
 *     <th>Path</th>
 *     <th>Description</th>
 *   </tr>
 *   <tr>
 *     <td>GET</td>
 *     <td>/api/patterns/list</td>
 *     <td>List all patterns (0-22)</td>
 *   </tr>
 *   <tr>
 *     <td>GET</td>
 *     <td>/api/patterns/{id}</td>
 *     <td>Get pattern metadata</td>
 *   </tr>
 *   <tr>
 *     <td>GET</td>
 *     <td>/api/patterns/{id}/image</td>
 *     <td>Get pattern PNG image</td>
 *   </tr>
 *   <tr>
 *     <td>GET</td>
 *     <td>/pieces/{id}.png</td>
 *     <td>Get piece image (001-256)</td>
 *   </tr>
 * </table>
 *
 * <h2>WebSocket Updates</h2>
 * <pre>
 * // Connect to WebSocket
 * const socket = new SockJS('http://localhost:8080/ws');
 * const stompClient = Stomp.over(socket);
 *
 * // Subscribe to metrics updates
 * stompClient.subscribe('/topic/metrics', (message) => {
 *     const metrics = JSON.parse(message.body);
 *     // Update UI with new metrics
 * });
 * </pre>
 *
 * <h2>Database Schema</h2>
 * <pre>
 * Table: historical_metrics
 * ┌─────────────────┬──────────┬─────────────────────┐
 * │ Column          │ Type     │ Description         │
 * ├─────────────────┼──────────┼─────────────────────┤
 * │ id              │ BIGINT   │ Primary key         │
 * │ config_name     │ VARCHAR  │ Configuration ID    │
 * │ depth           │ INT      │ Current depth       │
 * │ progress_pct    │ DOUBLE   │ Progress %          │
 * │ compute_time_ms │ BIGINT   │ Total compute time  │
 * │ timestamp       │ DATETIME │ Record timestamp    │
 * │ status          │ VARCHAR  │ running/idle/solved │
 * │ pieces_per_sec  │ DOUBLE   │ Performance metric  │
 * └─────────────────┴──────────┴─────────────────────┘
 *
 * Indexes:
 * - config_name (for fast lookup)
 * - timestamp (for time-range queries)
 * </pre>
 *
 * <h2>Configuration</h2>
 * <p>
 * Configuration via application.properties:
 * <pre>
 * # Server
 * server.port=8080
 *
 * # H2 Database
 * spring.datasource.url=jdbc:h2:file:./data/monitoring-db
 * spring.jpa.hibernate.ddl-auto=update
 *
 * # File Watcher
 * monitoring.saves-directory=./saves
 * monitoring.file-watcher.enabled=true
 * monitoring.file-watcher.poll-interval=2000
 * </pre>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Start monitoring application
 * java -jar eternity-solver-1.0.0-monitoring.jar
 *
 * // Access dashboard
 * http://localhost:8080
 *
 * // API examples
 * curl http://localhost:8080/api/configs
 * curl http://localhost:8080/api/patterns/list
 * curl http://localhost:8080/api/stats/global
 * }</pre>
 *
 * <h2>Development</h2>
 * <h3>Backend (Spring Boot)</h3>
 * <pre>
 * mvn spring-boot:run
 * </pre>
 *
 * <h3>Frontend (React)</h3>
 * <pre>
 * cd frontend
 * npm install
 * npm run dev
 * </pre>
 *
 * <h2>Testing</h2>
 * <p>
 * Comprehensive test coverage:
 * <ul>
 *   <li>Controller integration tests with MockMvc</li>
 *   <li>Service unit tests with mocked dependencies</li>
 *   <li>Repository tests with H2 in-memory database</li>
 *   <li>WebSocket tests for real-time updates</li>
 * </ul>
 *
 * <pre>{@code
 * // Run all monitoring tests
 * mvn test -Dtest=monitoring.**
 *
 * // Run specific test
 * mvn test -Dtest=DashboardControllerIntegrationTest
 * }</pre>
 *
 * <h2>Performance</h2>
 * <ul>
 *   <li><b>File scanning</b>: Every 2 seconds (configurable)</li>
 *   <li><b>WebSocket broadcast</b>: Every 2 seconds when changes detected</li>
 *   <li><b>Database writes</b>: Batched for efficiency</li>
 *   <li><b>Memory footprint</b>: ~50MB for backend + database</li>
 * </ul>
 *
 * <h2>Swagger UI</h2>
 * <p>
 * Interactive API documentation available at:
 * <a href="http://localhost:8080/swagger-ui.html">http://localhost:8080/swagger-ui.html</a>
 *
 * <p>
 * Includes:
 * <ul>
 *   <li>All REST endpoints documented</li>
 *   <li>Request/response schemas</li>
 *   <li>Try-it-out functionality</li>
 *   <li>Example values for all models</li>
 * </ul>
 *
 * @see model.Board Board representation
 * @see util.SaveStateManager State persistence
 * @see solver.SolverStatistics Solver metrics
 * @since 1.0
 * @author Eternity Solver Team
 */
package monitoring;
