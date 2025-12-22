/**
 * Data models for monitoring dashboard.
 *
 * <h2>Main Models</h2>
 * <ul>
 *   <li>{@link monitoring.model.ConfigMetrics} - Configuration-level metrics</li>
 *   <li>{@link monitoring.model.HistoricalMetrics} - Historical metrics (JPA entity)</li>
 *   <li>Various DTOs for API responses</li>
 * </ul>
 *
 * <h2>Persistence</h2>
 * <p>Uses JPA/Hibernate with H2 database for historical metrics storage.</p>
 *
 * @see monitoring.repository
 */
package monitoring.model;
