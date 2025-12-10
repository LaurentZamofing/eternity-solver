package monitoring.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI configuration for Swagger UI documentation.
 *
 * <p>Provides interactive API documentation at:
 * <ul>
 *   <li>Swagger UI: {@code http://localhost:8080/swagger-ui.html}</li>
 *   <li>OpenAPI spec: {@code http://localhost:8080/v3/api-docs}</li>
 * </ul>
 *
 * <h2>Features:</h2>
 * <ul>
 *   <li>Interactive REST API explorer</li>
 *   <li>Try out endpoints with real requests</li>
 *   <li>View request/response schemas</li>
 *   <li>Authentication testing (if enabled)</li>
 * </ul>
 */
@Configuration
public class OpenAPIConfig {

    /**
     * Configure OpenAPI documentation metadata and servers.
     *
     * @return OpenAPI instance with custom configuration
     */
    @Bean
    public OpenAPI monitoringOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Eternity Solver Monitoring API")
                        .description("""
                                REST API for monitoring and managing Eternity puzzle solver progress.

                                ## Features
                                - Real-time solver metrics (depth, progress, compute time)
                                - Historical data tracking and charting
                                - Configuration management and comparison
                                - Pattern and piece visualization
                                - Cell-level puzzle state inspection
                                - WebSocket support for live updates

                                ## Usage
                                All endpoints return JSON responses with consistent formats.
                                Use the interactive Swagger UI to test endpoints directly.

                                ## WebSocket
                                For real-time updates, connect to:
                                - ws://localhost:8080/ws/metrics
                                - Subscribe to: /topic/metrics

                                ## Error Handling
                                All errors return standardized responses:
                                - 400: Bad Request (invalid parameters)
                                - 404: Not Found (resource doesn't exist)
                                - 500: Internal Server Error
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Eternity Solver Team")
                                .url("https://github.com/yourusername/eternity-solver"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Development server"),
                        new Server()
                                .url("http://localhost:8080")
                                .description("Production server (update URL for deployment)")
                ));
    }
}
