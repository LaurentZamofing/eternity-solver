/**
 * Backend services for monitoring dashboard.
 *
 * <h2>Main Services</h2>
 * <ul>
 *   <li>{@link monitoring.service.MetricsAggregator} - Aggregates solver metrics</li>
 *   <li>{@link monitoring.service.CellDetailsService} - Provides cell-level analysis</li>
 *   <li>{@link monitoring.service.HistoricalCellDetailsService} - Historical cell data</li>
 *   <li>{@link monitoring.service.SaveFileParser} - Parses save files for metrics</li>
 *   <li>{@link monitoring.service.PieceDefinitionService} - Manages piece definitions</li>
 *   <li>{@link monitoring.service.FileSystemWatcher} - Watches for save file changes</li>
 *   <li>{@link monitoring.service.FileWatcherServiceImpl} - File watching implementation</li>
 *   <li>{@link monitoring.service.HistoricalDataBackfiller} - Backfills historical metrics</li>
 *   <li>{@link monitoring.service.StatsFileProcessor} - Processes statistics files</li>
 * </ul>
 *
 * @see monitoring.controller.DashboardController
 */
package monitoring.service;
