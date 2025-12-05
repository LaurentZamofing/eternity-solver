package monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Boot main application for Eternity Solver Monitoring Dashboard.
 *
 * Features:
 * - REST API for metrics querying
 * - WebSocket for real-time updates
 * - H2 database for historical data
 * - File watching for save directory
 *
 * Usage:
 *   java -jar eternity-solver-1.0.0-monitoring.jar
 *
 * Access dashboard at: http://localhost:8080
 * H2 Console: http://localhost:8080/h2-console
 */
@SpringBootApplication
@EnableScheduling
public class MonitoringApplication {

    private static final Logger logger = LoggerFactory.getLogger(MonitoringApplication.class);

    public static void main(String[] args) {
        logger.info("╔═══════════════════════════════════════════════════════════╗");
        logger.info("║  Eternity Solver - Monitoring Dashboard                  ║");
        logger.info("║  Starting Spring Boot Application...                     ║");
        logger.info("╚═══════════════════════════════════════════════════════════╝");

        SpringApplication.run(MonitoringApplication.class, args);

        logger.info("");
        logger.info("✅ Monitoring Dashboard is running!");
        logger.info("📊 Dashboard: http://localhost:8080");
        logger.info("🔌 WebSocket: ws://localhost:8080/ws");
        logger.info("💾 H2 Console: http://localhost:8080/h2-console");
        logger.info("");
        logger.info("Press Ctrl+C to stop");
    }
}
