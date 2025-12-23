# Monitoring System Architecture

## Overview

The Eternity Solver Monitoring System provides real-time visualization and tracking of the puzzle solver's progress. It consists of a Spring Boot backend that watches save files and a React frontend that displays live metrics through WebSocket connections.

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         FRONTEND LAYER                          │
│                      (React + TypeScript)                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐        │
│  │  App.tsx     │  │ BoardViz     │  │ ConfigTable  │        │
│  │ (Main State) │  │              │  │              │        │
│  └──────┬───────┘  └──────────────┘  └──────────────┘        │
│         │                                                       │
│  ┌──────▼────────────────────────────┐                        │
│  │   WebSocket Service (STOMP)       │                        │
│  │   + API Service (Axios)           │                        │
│  └──────┬────────────────────────────┘                        │
│         │                                                       │
└─────────┼───────────────────────────────────────────────────────┘
          │ HTTP/WS
          │
┌─────────▼───────────────────────────────────────────────────────┐
│                      BACKEND LAYER                              │
│                   (Spring Boot 3.x)                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │               CONTROLLER LAYER                          │  │
│  ├─────────────────────────────────────────────────────────┤  │
│  │  • DashboardController (REST API)                       │  │
│  │  • MetricsWebSocketController (WebSocket STOMP)         │  │
│  │  • PatternController (Static assets)                    │  │
│  └──────────────────────┬──────────────────────────────────┘  │
│                         │                                       │
│  ┌──────────────────────▼───────────────────────────────────┐ │
│  │               SERVICE LAYER                              │ │
│  ├──────────────────────────────────────────────────────────┤ │
│  │  File Monitoring:                                        │ │
│  │    • IFileWatcherService / FileWatcherServiceImpl       │ │
│  │    • SaveFileParser                                      │ │
│  │    • MetricsCacheManager                                 │ │
│  │    • BestDepthCalculator                                 │ │
│  │                                                           │ │
│  │  Metrics & Analytics:                                    │ │
│  │    • IMetricsAggregator / MetricsAggregator             │ │
│  │    • CellDetailsService                                  │ │
│  │    • HistoricalCellDetailsService                        │ │
│  │                                                           │ │
│  │  Data:                                                    │ │
│  │    • PieceDefinitionService                              │ │
│  └──────────────────────┬───────────────────────────────────┘ │
│                         │                                       │
│  ┌──────────────────────▼───────────────────────────────────┐ │
│  │            PERSISTENCE LAYER                             │ │
│  ├──────────────────────────────────────────────────────────┤ │
│  │  • MetricsRepository (Spring Data JPA)                   │ │
│  │  • HistoricalMetrics (Entity)                            │ │
│  │  • H2 Database (In-Memory)                               │ │
│  └──────────────────────────────────────────────────────────┘ │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
          │
          │ File System Watch
          │
┌─────────▼───────────────────────────────────────────────────────┐
│                      FILE SYSTEM                                │
├─────────────────────────────────────────────────────────────────┤
│  saves/eternity2/                                               │
│    ├── eternity2_p01_ascending/                                 │
│    │   ├── current_1731868234567.txt                            │
│    │   ├── best_176.txt                                         │
│    │   └── stats_history.jsonl                                  │
│    └── ... (48 configurations)                                  │
│                                                                 │
│  data/eternity2/                                                │
│    ├── eternity2.txt (piece definitions)                        │
│    └── images/ (pattern images)                                 │
└─────────────────────────────────────────────────────────────────┘
```

## Technology Stack

### Backend
- **Framework:** Spring Boot 3.x
- **WebSocket:** STOMP protocol over SockJS
- **Database:** H2 (in-memory, persistent mode)
- **ORM:** Spring Data JPA
- **Scheduling:** Spring @Scheduled
- **Logging:** SLF4J + Logback

### Frontend
- **Framework:** React 18 with TypeScript 5.x
- **Build Tool:** Vite 5.x
- **Styling:** Tailwind CSS 3.x
- **Charts:** Chart.js 4.x
- **WebSocket:** STOMP.js over SockJS
- **HTTP Client:** Axios
- **Testing:** Vitest + React Testing Library

## Component Responsibilities

### Backend Services

#### FileWatcherServiceImpl
**Location:** `src/main/java/monitoring/service/FileWatcherServiceImpl.java`

**Responsibilities:**
- Monitor `saves/eternity2/` directory for file changes
- Trigger parsing when save files are modified
- Maintain in-memory cache of latest metrics
- Publish updates via WebSocket
- Persist metrics to database
- Schedule periodic health checks

**Key Methods:**
- `onFileChange(Path path)` - React to file system events
- `processConfigDirectory(Path configDir)` - Parse all saves for a config
- `publishMetricsUpdate(ConfigMetrics)` - Broadcast via WebSocket
- `getAllMetrics()` - Return cached metrics
- `refreshCache()` - Full rescan

**Dependencies:**
- SaveFileParser
- MetricsCacheManager
- BestDepthCalculator
- MetricsRepository
- SimpMessagingTemplate (WebSocket)

#### SaveFileParser
**Location:** `src/main/java/monitoring/service/SaveFileParser.java`

**Responsibilities:**
- Parse save file format (headers + board state)
- Extract metrics (depth, progress, compute time)
- Parse board state array
- Parse placement order
- Extract statistics

**Key Methods:**
- `parseSaveFile(Path file)` - Main parsing entry point
- `parseHeaders(List<String>)` - Extract metadata
- `parseBoardState(List<String>)` - Parse grid
- `parsePlacementOrder(List<String>)` - Parse history

#### MetricsCacheManager
**Location:** `src/main/java/monitoring/service/MetricsCacheManager.java`

**Responsibilities:**
- Thread-safe metrics caching (ConcurrentHashMap)
- Cache CRUD operations
- Cache retrieval for WebSocket broadcasts

**Key Methods:**
- `put(String configName, ConfigMetrics)` - Update cache
- `get(String configName)` - Retrieve cached metrics
- `getAll()` - Get all cached metrics
- `remove(String configName)` - Remove from cache
- `clear()` - Clear entire cache

#### BestDepthCalculator
**Location:** `src/main/java/monitoring/service/BestDepthCalculator.java`

**Responsibilities:**
- Calculate best depth ever reached per configuration
- Scan `best_*.txt` files in save directories
- Extract depth numbers from filenames

**Key Methods:**
- `calculateBestDepthEver(String configName)` - Scan and calculate max depth

#### MetricsAggregator
**Location:** `src/main/java/monitoring/service/MetricsAggregator.java`

**Responsibilities:**
- Aggregate metrics across all configurations
- Calculate global statistics
- Identify active vs. stalled configurations
- Compute totals and averages

**Key Methods:**
- `aggregateMetrics(Map<String, ConfigMetrics>)` - Compute GlobalStats
- Statistical calculations (total depth, avg progress, etc.)

#### CellDetailsService
**Location:** `src/main/java/monitoring/service/CellDetailsService.java`

**Responsibilities:**
- Calculate detailed information for a specific cell
- Determine which pieces can fit at a position
- Apply constraint checking (borders, neighbors)
- Calculate valid/invalid possibilities
- Track placement strategy (ascending/descending)

**Key Methods:**
- `getCellDetails(String configName, int row, int col)` - Get current cell details
- `getCellDetailsFromMetrics(ConfigMetrics, int row, int col)` - Get from historical state
- `calculateCellDetailsCore(...)` - Core calculation logic
- `calculatePossibilities(...)` - Calculate all valid piece placements
- `fits(Board, int row, int col, int[] edges)` - Validate piece placement

#### HistoricalCellDetailsService
**Location:** `src/main/java/monitoring/service/HistoricalCellDetailsService.java`

**Responsibilities:**
- Retrieve cell details from historical snapshots
- Query database for past metrics
- Reconstruct board state at specific timestamps

**Key Methods:**
- `getCellDetailsAtTimestamp(String configName, long timestamp, int row, int col)`

#### PieceDefinitionService
**Location:** `src/main/java/monitoring/service/PieceDefinitionService.java`

**Responsibilities:**
- Load piece definitions from data files
- Cache piece edge configurations
- Provide piece lookup by ID

**Key Methods:**
- `getPieceDefinitions(String puzzleName)` - Load and cache piece definitions
- `loadPieceDefinitions(Path file)` - Parse piece definition file

### Backend Controllers

#### DashboardController
**Location:** `src/main/java/monitoring/controller/DashboardController.java`

**REST API Endpoints:**

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/configs` | List all configurations |
| GET | `/api/configs/{name}` | Get specific config metrics |
| GET | `/api/configs/{name}/cell/{row}/{col}` | Get cell details |
| GET | `/api/configs/{name}/history` | Get historical metrics |
| GET | `/api/global-stats` | Get global statistics |
| POST | `/api/refresh-cache` | Trigger cache refresh |

**Query Parameters:**
- `sortBy` - Sort field (name, progress, depth, lastUpdate)
- `order` - Sort order (asc, desc)
- `status` - Filter by status (all, active, stalled)
- `hours` - Historical data window (default: 24)
- `limit` - Maximum records (default: 1000)

#### MetricsWebSocketController
**Location:** `src/main/java/monitoring/controller/MetricsWebSocketController.java`

**WebSocket Topics:**

| Type | Destination | Description |
|------|-------------|-------------|
| Subscribe | `/topic/metrics` | Real-time metrics updates |
| Subscribe | `/topic/stats` | Global statistics updates |
| Message | `/app/getConfig` | Request specific config |
| Message | `/app/ping` | Keep-alive ping |

**Subscription Behavior:**
- New subscribers immediately receive current state
- File changes trigger broadcasts to all subscribers
- Automatic reconnection on disconnect

#### PatternController
**Location:** `src/main/java/monitoring/controller/PatternController.java`

**Static Asset Endpoints:**

| Method | Path | Description |
|--------|------|-------------|
| GET | `/patterns/{puzzleName}/{patternId}.png` | Get pattern image |
| GET | `/pieces/{pieceId}.png` | Get pre-rendered piece image |
| GET | `/api/patterns/{puzzleName}` | List all pattern IDs |

### Frontend Components

#### App.tsx
**Location:** `frontend/src/components/App.tsx`

**Responsibilities:**
- WebSocket connection lifecycle management
- Global state management (configs, stats)
- Routing and navigation
- Error handling and retry logic
- Auto-refresh scheduling

**State:**
- `configs` - All configuration metrics
- `globalStats` - Aggregated statistics
- `wsConnected` - WebSocket connection status
- `isRefreshing` - Cache refresh state
- `error` - Error messages

**Effects:**
- Connect to WebSocket on mount
- Subscribe to `/topic/metrics` and `/topic/stats`
- Periodic stats reload (30s interval)
- Cleanup on unmount

#### BoardVisualizer.tsx
**Location:** `frontend/src/components/BoardVisualizer.tsx`

**Responsibilities:**
- Render puzzle grid with piece placements
- Display piece IDs and rotations
- Handle cell click events
- Show empty cells
- Responsive sizing

**Props:**
- `boardState: string[][]` - Board configuration
- `rows: number` - Grid height
- `cols: number` - Grid width
- `onCellClick?: (row, col) => void` - Cell click handler

#### ConfigTable.tsx
**Location:** `frontend/src/components/ConfigTable.tsx`

**Responsibilities:**
- Display all configurations in table format
- Sort by multiple columns
- Filter by status (active/stalled/all)
- Show progress bars
- Format time values
- Row selection for details

**Props:**
- `configs: ConfigMetrics[]` - Configuration list
- `onConfigClick?: (config) => void` - Row click handler

#### PieceVisualizer.tsx
**Location:** `frontend/src/components/PieceVisualizer.tsx`

**Responsibilities:**
- Render individual puzzle piece
- Display edge patterns (4 triangles)
- Apply rotation transforms
- Show piece ID label
- Load pattern images from backend

**Props:**
- `pieceId: number` - Piece identifier
- `north/east/south/west: number` - Edge pattern IDs
- `rotation: number` - Rotation (0, 1, 2, 3)
- `showLabel: boolean` - Display piece ID

**Implementation:**
- Uses pre-rendered piece images from `/pieces/{pieceId}.png`
- Applies CSS rotation transforms
- Counter-rotates label for readability

#### CellDetailsModal.tsx
**Location:** `frontend/src/components/CellDetailsModal.tsx`

**Responsibilities:**
- Display detailed cell information in modal
- Show current piece placement
- List all possible pieces (valid/invalid)
- Display constraints (borders, neighbors)
- Show statistics

**Props:**
- `configName: string` - Configuration name
- `row: number` - Cell row
- `col: number` - Cell column
- `isOpen: boolean` - Modal visibility
- `onClose: () => void` - Close handler

#### HistoricalChart.tsx
**Location:** `frontend/src/components/HistoricalChart.tsx`

**Responsibilities:**
- Render time-series chart (Chart.js)
- Plot depth progress over time
- Support zoom and pan
- Responsive layout

**Props:**
- `configName: string` - Configuration name
- `hours?: number` - Time window (default: 24)

### Frontend Services

#### api.ts
**Location:** `frontend/src/services/api.ts`

**Responsibilities:**
- HTTP client using Axios
- REST API wrapper functions
- Error handling and retries
- Request/response typing

**Key Functions:**
- `getAllConfigs(sortBy?, order?, status?)` - Fetch all configs
- `getConfig(name)` - Fetch specific config
- `getCellDetails(name, row, col)` - Fetch cell details
- `getHistoricalMetrics(name, hours?, limit?)` - Fetch history
- `getGlobalStats()` - Fetch global stats
- `refreshCache()` - Trigger cache refresh

#### websocket.ts
**Location:** `frontend/src/services/websocket.ts`

**Responsibilities:**
- WebSocket connection management (STOMP over SockJS)
- Topic subscription handling
- Connection lifecycle (connect, disconnect, reconnect)
- Event callbacks

**Key Functions:**
- `connect()` - Establish WebSocket connection
- `disconnect()` - Close WebSocket connection
- `subscribe(topic, callback)` - Subscribe to topic
- `onConnect(callback)` - Register connect handler
- `onDisconnect(callback)` - Register disconnect handler
- `onError(callback)` - Register error handler

**Connection Flow:**
1. Create SockJS socket: `new SockJS('http://localhost:8080/ws')`
2. Create STOMP client: `Stomp.over(socket)`
3. Connect with callbacks
4. Subscribe to topics: `/topic/metrics`, `/topic/stats`
5. Handle incoming messages
6. Cleanup on disconnect

## Data Flow

### Real-Time Metrics Update Flow

```
1. Solver writes to save file
   └─> saves/eternity2/eternity2_p01_ascending/current_123456789.txt

2. FileWatcherServiceImpl detects change
   └─> @Scheduled health check or file system event

3. SaveFileParser parses file
   └─> Extracts headers, board state, placement order
   └─> Returns ConfigMetrics object

4. MetricsCacheManager updates cache
   └─> Thread-safe update in ConcurrentHashMap

5. FileWatcherServiceImpl persists to database
   └─> MetricsRepository.save(HistoricalMetrics)

6. FileWatcherServiceImpl broadcasts via WebSocket
   └─> SimpMessagingTemplate.convertAndSend("/topic/metrics", metrics)

7. Frontend receives WebSocket message
   └─> STOMP client callback triggered
   └─> App.tsx updates state
   └─> React re-renders components
```

### REST API Request Flow

```
1. Frontend makes HTTP request
   └─> apiService.getAllConfigs()

2. Axios sends GET request
   └─> GET /api/configs?sortBy=progress&order=desc

3. DashboardController receives request
   └─> @GetMapping("/api/configs")

4. Controller calls FileWatcherService
   └─> fileWatcherService.getAllMetrics()

5. MetricsCacheManager returns cached data
   └─> Map<String, ConfigMetrics>

6. Controller applies sorting/filtering
   └─> Sort by specified field
   └─> Filter by status if requested

7. Controller returns response
   └─> ResponseEntity<List<ConfigMetrics>>

8. Frontend receives response
   └─> Updates component state
   └─> Re-renders UI
```

### Cell Details Request Flow

```
1. User clicks cell in BoardVisualizer
   └─> onCellClick(row, col) callback

2. Frontend fetches cell details
   └─> apiService.getCellDetails(configName, row, col)

3. DashboardController receives request
   └─> GET /api/configs/{name}/cell/{row}/{col}

4. Controller calls CellDetailsService
   └─> cellDetailsService.getCellDetails(configName, row, col)

5. CellDetailsService processes
   ├─> Load save file (SaveFileParser)
   ├─> Build Board object
   ├─> Load piece definitions (PieceDefinitionService)
   ├─> Calculate constraints
   ├─> Calculate all possibilities (4 rotations × N pieces)
   ├─> Check validity (fits())
   └─> Return CellDetails object

6. Frontend receives response
   └─> Opens CellDetailsModal
   └─> Displays current piece, possibilities, constraints
```

## Configuration

### Application Properties

**Location:** `src/main/resources/application.properties`

```properties
# Server
server.port=8080

# H2 Database
spring.datasource.url=jdbc:h2:file:./data/monitoring-db
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA/Hibernate
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=update

# H2 Console
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# Logging
logging.level.monitoring=DEBUG
logging.level.org.springframework.messaging=DEBUG

# File Watching
monitoring.saves-directory=./saves
```

### Constants

**Location:** `src/main/java/monitoring/MonitoringConstants.java`

Centralized configuration constants:

```java
// File parsing
MAX_LINES_TO_READ = 500
FILE_WRITE_DELAY_MS = 100

// API defaults
DEFAULT_HISTORY_LIMIT = 1000
DEFAULT_HISTORY_HOURS = 24
DEFAULT_RECENT_HOURS = 1

// Time
SECONDS_PER_DAY = 86400
HEALTH_CHECK_INTERVAL_MS = 300000
SHUTDOWN_TIMEOUT_SECONDS = 5

// Database
CONFIG_NAME_MAX_LENGTH = 100

// HTTP Status
INTERNAL_SERVER_ERROR = 500
BAD_REQUEST = 400
```

## Data Models

### ConfigMetrics
**Location:** `src/main/java/monitoring/model/ConfigMetrics.java`

Primary metrics model:

```java
public class ConfigMetrics {
    private String configName;
    private int depth;              // Pieces placed
    private int totalPieces;        // Total pieces
    private double progress;        // Percentage
    private long computeTimeSeconds;
    private long lastUpdate;        // Timestamp
    private String[][] boardState;  // Grid state
    private int rows;
    private int cols;
    private int bestDepthEver;      // Historical max
    private String status;          // "active", "stalled"
    // ... getters, setters, methods
}
```

### CellDetails
**Location:** `src/main/java/monitoring/model/CellDetails.java`

Cell analysis model:

```java
public class CellDetails {
    private int row;
    private int col;
    private PieceOption currentPiece;
    private List<PieceOption> possiblePieces;
    private CellConstraints constraints;
    private Statistics statistics;

    public static class Statistics {
        private int totalPieces;
        private int usedPieces;
        private int validOptions;
        private int invalidOptions;
    }
}
```

### PieceOption
**Location:** `src/main/java/monitoring/model/PieceOption.java`

Piece possibility model:

```java
public class PieceOption {
    private int pieceId;
    private int rotation;           // 0, 1, 2, 3
    private int[] edges;            // [N, E, S, W]
    private boolean isValid;        // Fits constraints
    private boolean isCurrent;      // Currently placed
    private boolean alreadyTried;   // Based on strategy
}
```

### CellConstraints
**Location:** `src/main/java/monitoring/model/CellConstraints.java`

Constraint information:

```java
public class CellConstraints {
    private Map<String, Boolean> borders;  // N, E, S, W
    private Map<String, NeighborInfo> neighbors;

    public static class NeighborInfo {
        private int row;
        private int col;
        private int pieceId;
        private int rotation;
        private int edgeValue;  // Required match
    }
}
```

### HistoricalMetrics
**Location:** `src/main/java/monitoring/model/HistoricalMetrics.java`

Database entity:

```java
@Entity
@Table(name = "historical_metrics")
public class HistoricalMetrics {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String configName;

    @Column(nullable = false)
    private int depth;

    @Column(nullable = false)
    private double progress;

    @Column(nullable = false)
    private long timestamp;

    @Column(columnDefinition = "TEXT")
    private String boardStateJson;
}
```

## Design Patterns

### Service Layer Pattern
- Clear separation between controllers and business logic
- Services are interface-based for testability (IFileWatcherService, IMetricsAggregator)
- Dependency injection via @Autowired

### Repository Pattern
- Spring Data JPA for database access
- MetricsRepository extends JpaRepository
- Query methods defined by naming convention

### Observer Pattern (Pub/Sub)
- FileWatcherService publishes events
- WebSocket clients subscribe to topics
- Decoupled communication

### Cache-Aside Pattern
- MetricsCacheManager maintains in-memory cache
- Read: Check cache → Load from file if miss → Update cache
- Write: Update file → Update cache → Broadcast

### Singleton Services
- Spring @Service beans are singletons by default
- Thread-safe caching with ConcurrentHashMap
- Stateless services where possible

## Threading Model

### Backend

**File Watching:**
- @Scheduled tasks run on Spring's default task executor
- Health check every 5 minutes (HEALTH_CHECK_INTERVAL_MS)
- Thread-safe cache operations

**WebSocket:**
- STOMP broker uses separate thread pool
- Message broadcasting is async
- Client connections handled by Tomcat thread pool

**Database:**
- JPA operations on request threads
- Connection pooling by HikariCP (default)

### Frontend

**WebSocket:**
- Single WebSocket connection
- STOMP client handles message dispatch
- Callbacks execute on main thread (React state updates)

**Rendering:**
- React reconciliation on main thread
- Chart.js animations use requestAnimationFrame
- Vite HMR on separate WebSocket (development only)

## Performance Considerations

### Backend Optimizations

**File Parsing:**
- Limited to MAX_LINES_TO_READ (500 lines)
- Early exit after parsing placement order
- Delay (FILE_WRITE_DELAY_MS) to ensure file is fully written

**Caching:**
- In-memory cache (MetricsCacheManager) for instant reads
- No disk I/O on metric queries
- Cache invalidated on file changes

**Database:**
- Bulk inserts for historical metrics
- Indexes on (configName, timestamp)
- H2 in-memory mode for speed

**WebSocket:**
- Broadcast only on changes (not periodic)
- Single message to all subscribers
- No per-client processing

### Frontend Optimizations

**React:**
- Functional components (hooks) for performance
- Memoization candidates: BoardVisualizer, ConfigTable
- Virtual scrolling for large lists (future)

**WebSocket:**
- Single connection reused
- Message batching by STOMP
- Automatic reconnection with exponential backoff

**Assets:**
- Pre-rendered piece images (server-side)
- Pattern images cached by browser
- Vite code splitting

## Testing Strategy

### Backend Tests

**Unit Tests:**
- Service layer (SaveFileParser, MetricsAggregator)
- Mockito for dependencies
- JUnit 5

**Integration Tests:**
- Controller tests with @WebMvcTest
- WebSocket tests with TestRestTemplate
- H2 in-memory database

**Test Coverage:**
- Target: 70%+ for service layer
- Core logic: 90%+
- Controllers: 60%+

### Frontend Tests

**Component Tests:**
- Vitest + React Testing Library
- User interaction simulation
- Mock API and WebSocket services

**Integration Tests:**
- App.tsx full lifecycle
- WebSocket connection flow
- Error handling

**Test Coverage:**
- Current: 88% (104/118 tests passing)
- Target: 80%+ overall
- Critical paths: 100%

## Security Considerations

### Authentication
**Current:** None (development/local use)
**Production:** Add Spring Security with JWT tokens

### CORS
**Current:** Allowed all origins (`setAllowedOriginPatterns("*")`)
**Production:** Restrict to specific domains

### Input Validation
- Path traversal prevention in file operations
- Input sanitization in REST endpoints
- SQL injection protected by JPA

### WebSocket Security
**Current:** Open connection
**Production:** Add STOMP authentication and user-specific topics

## Deployment

### Backend

**JAR Build:**
```bash
./mvnw clean package
java -jar target/eternity-solver-1.0.0-monitoring.jar
```

**Docker:**
```dockerfile
FROM eclipse-temurin:17-jre
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Frontend

**Production Build:**
```bash
cd frontend
npm run build
# Output: dist/
```

**Integration Options:**
1. Serve from Spring Boot static resources
2. Deploy to CDN (CloudFront, Cloudflare)
3. Nginx reverse proxy

## Monitoring and Observability

### Logging

**Levels:**
- DEBUG: File parsing, cache operations
- INFO: WebSocket connections, major events
- WARN: Missing files, parse errors
- ERROR: Exceptions, failures

**Format:**
```
2025-12-10 14:32:15 [main] INFO  monitoring.MonitoringApplication - Dashboard is running!
```

### Metrics (Future)

Consider adding:
- Micrometer for metrics export
- Prometheus endpoint
- Grafana dashboards
- JVM metrics (heap, GC)
- Request rates and latency

### Health Checks

**Spring Boot Actuator:**
```properties
management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=always
```

**Endpoints:**
- `/actuator/health` - Application health
- `/actuator/info` - Build information

## Future Enhancements

### Backend
1. **Authentication & Authorization**
   - Spring Security integration
   - User roles (viewer, admin)
   - API key authentication

2. **Advanced Metrics**
   - Backtrack frequency
   - Branch factor analysis
   - Dead-end detection patterns

3. **Database Optimization**
   - PostgreSQL for production
   - Partitioning by date
   - Archive old metrics

4. **Caching**
   - Redis for distributed cache
   - Cache eviction policies
   - Warm-up on startup

5. **API Enhancements**
   - GraphQL endpoint
   - Pagination for large results
   - OpenAPI/Swagger documentation

### Frontend
1. **State Management**
   - Zustand or Jotai for complex state
   - Persistent state (localStorage)

2. **Performance**
   - Virtual scrolling (react-window)
   - Lazy loading components
   - Image optimization

3. **Features**
   - Compare multiple configs side-by-side
   - Export data (CSV, JSON)
   - Custom alerts and notifications
   - Dark mode

4. **Visualization**
   - 3D board view
   - Heatmaps for piece difficulty
   - Animation of solving progress

## Troubleshooting

### Common Issues

**WebSocket not connecting:**
- Check backend is running on port 8080
- Verify CORS configuration
- Check browser console for errors
- Test with: `wscat -c ws://localhost:8080/ws`

**No metrics displayed:**
- Check saves directory exists: `./saves/eternity2/`
- Verify save files have correct format
- Check backend logs for parsing errors
- Refresh cache: POST `/api/refresh-cache`

**Slow performance:**
- Check file parsing (MAX_LINES_TO_READ)
- Monitor H2 database size
- Check WebSocket broadcast frequency
- Profile with JProfiler or VisualVM

**Database errors:**
- Check H2 file permissions
- Verify schema with H2 Console
- Check disk space
- Reset database: Delete `data/monitoring-db.*`

## References

### Documentation
- [API.md](./API.md) - Complete REST and WebSocket API reference
- [DEVELOPER_GUIDE.md](./DEVELOPER_GUIDE.md) - Development setup and guidelines
- [frontend/README.md](../frontend/README.md) - Frontend architecture and development

### External Resources
- [Spring Boot](https://spring.io/projects/spring-boot)
- [Spring WebSocket](https://docs.spring.io/spring-framework/reference/web/websocket.html)
- [React Documentation](https://react.dev)
- [STOMP Protocol](https://stomp.github.io/)
- [Chart.js](https://www.chartjs.org/)

## Changelog

**2025-12-10:** Initial architecture documentation created
- Complete system overview
- Component responsibilities
- Data flow diagrams
- Design patterns
- Performance considerations
- Testing strategy
- Deployment guide
