# Eternity Solver - Real-Time Monitoring System

## 📊 Overview

A comprehensive real-time monitoring dashboard for tracking multiple Eternity puzzle solver configurations simultaneously. The system provides live metrics, historical charts, and performance analytics through a modern web interface.

## ✨ Features

### Real-Time Monitoring
- **Live WebSocket Updates**: Instant metric updates as configurations progress
- **Multi-Configuration View**: Monitor 15+ configurations simultaneously
- **Connection Status**: Visual indicator for backend connectivity

### Advanced Metrics
- **Physical Progress %**: Actual pieces placed (depth / totalPieces × 100)
- **Search Space Progress**: Solver's estimation of explored search space
- **Delta (Δ)**: Difference between search space and physical progress
  - 🔴 High Delta (>30%): Configuration likely impossible
  - 🟠 Moderate Delta (10-30%): Heavy pruning, slowing progress
  - ⚪ Low Delta (<10%): Healthy progress

### Performance Tracking
- **Pieces per Second**: Real-time solve speed
- **Compute Time**: Total time spent per configuration
- **ETA**: Estimated time to completion
- **Status Indicators**: Running, Solved, Stuck, Milestone

### Historical Analysis
- **Interactive Charts**: Chart.js visualization with multiple axes
- **Time Range Selection**: 1 hour to 1 week
- **Trend Analysis**: Track depth, progress, and speed over time
- **Statistical Summaries**: Max depth, average speed, data points

### Global Statistics
- Total configurations monitored
- Running/Solved/Stuck counts
- Best performer identification
- Cumulative compute time
- Top 5 configurations leaderboard

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────┐
│                   Browser (React)                    │
│  ┌─────────────┐  ┌──────────────┐  ┌────────────┐ │
│  │  ConfigTable│  │  GlobalStats │  │  Historical│ │
│  │             │  │              │  │   Charts   │ │
│  └─────────────┘  └──────────────┘  └────────────┘ │
│         │                 │                 │        │
│         └─────────────────┴─────────────────┘        │
│                           │                          │
│              ┌────────────┴────────────┐             │
│              │   WebSocket (STOMP)     │             │
│              │   REST API (Axios)      │             │
│              └────────────┬────────────┘             │
└───────────────────────────┼──────────────────────────┘
                            │
┌───────────────────────────┼──────────────────────────┐
│          Spring Boot Backend (Port 8080)             │
│  ┌──────────────────┐  ┌─────────────────────────┐  │
│  │ DashboardController│  │ MetricsWebSocketController│ │
│  │   (REST API)      │  │    (WebSocket/STOMP)    │  │
│  └────────┬──────────┘  └──────────┬──────────────┘  │
│           │                         │                 │
│  ┌────────┴─────────────────────────┴──────────┐     │
│  │          FileWatcherService                  │     │
│  │  (Java WatchService - monitors saves/)      │     │
│  └────────┬─────────────────────────┬──────────┘     │
│           │                         │                 │
│  ┌────────┴──────────┐    ┌────────┴──────────┐     │
│  │  SaveFileParser   │    │ MetricsAggregator │     │
│  │  (Regex parsing)  │    │   (Statistics)    │     │
│  └───────────────────┘    └───────────────────┘     │
│           │                                           │
│  ┌────────┴──────────────────────────────────┐      │
│  │        H2 Database (data/monitoring-db)   │      │
│  │      (Historical metrics persistence)      │      │
│  └────────────────────────────────────────────┘      │
└──────────────────────────────────────────────────────┘
                            │
┌───────────────────────────┼──────────────────────────┐
│                    File System                        │
│  saves/                                               │
│  ├── eternity2/                                       │
│  │   ├── eternity2_p01_ascending/                    │
│  │   │   ├── current_1234567890.txt                  │
│  │   │   └── best_178.txt                            │
│  │   ├── eternity2_p02_descending/                   │
│  │   │   └── ...                                      │
│  │   └── ...                                          │
└───────────────────────────────────────────────────────┘
```

## 🚀 Quick Start

### Prerequisites
- Java 11+
- Node.js 18+ and npm
- Maven 3.6+

### 1. Start the Backend

```bash
# Option 1: Maven (development)
mvn spring-boot:run

# Option 2: JAR (production)
mvn clean package -DskipTests
java -jar target/eternity-solver-1.0.0-monitoring.jar
```

Backend will start on **http://localhost:8080**

### 2. Start the Frontend

```bash
cd frontend
npm install  # First time only
npm run dev
```

Frontend will start on **http://localhost:3000**

### 3. Start the Solver

```bash
# In another terminal
mvn exec:java -Dexec.mainClass="MainParallel"
```

The solver will create save files in `saves/` that the monitoring system will automatically detect and display.

## 📡 API Endpoints

### REST API

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/configs` | GET | List all configurations with metrics |
| `/api/configs/{name}` | GET | Get specific configuration details |
| `/api/configs/{name}/history` | GET | Get historical data (query: hours, limit) |
| `/api/stats/global` | GET | Get global statistics |
| `/api/stats/recent` | GET | Get recent activity (query: hours) |
| `/api/health` | GET | Health check endpoint |

**Query Parameters:**
- `sort`: field to sort by (progress, depth, time, name)
- `order`: asc or desc (default: desc)
- `status`: filter by status (running, idle, solved, stuck)

### WebSocket

**Endpoint**: `ws://localhost:8080/ws`

**Topics:**
- `/topic/metrics/{configName}` - Individual config updates
- `/topic/stats/global` - Global statistics updates

**Subscribe to updates:**
```typescript
const client = new Client({
  brokerURL: 'ws://localhost:8080/ws',
  onConnect: () => {
    client.subscribe('/topic/metrics/eternity2_p01_ascending', (message) => {
      const metrics = JSON.parse(message.body);
      console.log('Received update:', metrics);
    });
  }
});
```

## 📂 File Format

Save files in `saves/` directory follow this format:

```
# Eternity II Save
# Timestamp: 1764667870757
# Date: 2025-12-02_10-31-10
# Puzzle: eternity2_p01_descending
# Dimensions: 16x16
# Depth: 178 (pieces placed by backtracking, excluding fixed)
# Progress: 100,00000000% (estimate based on first 5 depths)
# TotalComputeTime: 25511 ms (0h 00m 25s)

# Board data follows...
```

**File Naming Convention:**
- `current_<timestamp>.txt` - Latest state (status: "active")
- `best_<depth>.txt` - Milestone saves (status: "milestone")

## 🎨 Dashboard Components

### 1. Global Stats Panel
- Total configurations
- Running/Solved/Stuck counts
- Best progress indicator
- Average progress
- Max depth achieved
- Total compute time
- Top 5 performers leaderboard

### 2. Metrics Legend
Interactive info panel explaining:
- Physical vs Search Space progress
- Delta interpretation
- Color coding meanings

### 3. Configuration Table
Sortable, filterable table showing:
- Configuration name
- Depth (pieces placed / total)
- Physical progress % (blue bar)
- Search space progress % (green/red bar)
- Delta with warning indicators
- Compute time
- Speed (pieces/sec)
- Status badge
- View Details action

### 4. Historical Chart (in Details Modal)
- Multi-axis line chart (Depth, Progress, Speed)
- Time range selector (1h, 6h, 24h, 3d, 1w)
- Interactive tooltips
- Statistics summary cards

## ⚙️ Configuration

### Backend (`application.properties`)

```properties
# Server
server.port=8080

# H2 Database
spring.datasource.url=jdbc:h2:file:./data/monitoring-db;AUTO_SERVER=TRUE
spring.datasource.driverClassName=org.h2.Driver
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false

# Monitoring
monitoring.saves-directory=./saves
monitoring.watch-interval-seconds=5

# Logging
logging.level.monitoring=DEBUG
```

### Frontend (`vite.config.ts`)

```typescript
export default defineConfig({
  plugins: [react()],
  define: {
    global: 'globalThis', // SockJS polyfill
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/ws': {
        target: 'http://localhost:8080',
        ws: true,
      },
    },
  },
});
```

## 🔧 Troubleshooting

### Backend won't start
```bash
# Check if port 8080 is available
lsof -i :8080

# Kill process if needed
kill -9 <PID>

# Force Maven dependency refresh
mvn clean install -U
```

### Frontend connection errors
```bash
# Check backend is running
curl http://localhost:8080/api/health

# Clear browser cache and hard reload
# Chrome/Edge: Ctrl+Shift+R
# Firefox: Ctrl+F5
```

### No data appearing
- Verify solver is running and creating files in `saves/`
- Check backend logs for parser errors
- Ensure save files have proper header format
- Check file permissions on `saves/` directory

### WebSocket disconnects
- Check firewall settings
- Verify proxy configuration in `vite.config.ts`
- Check browser console for CORS errors

## 📊 Metrics Interpretation

### Physical Progress %
- **Definition**: `(depth / totalPieces) × 100`
- **Meaning**: Percentage of pieces actually placed on the board
- **Example**: 142/256 = 55.47%

### Search Space Progress %
- **Definition**: Solver's internal estimation
- **Meaning**: Percentage of search space explored (with pruning)
- **100%**: All branches explored/pruned - no more possibilities

### Delta (Δ)
- **Calculation**: `Search Space % - Physical %`
- **Interpretation**:
  - **+48.1% ⚠️**: Very high - configuration likely impossible
  - **+15.3%**: Moderate - heavy constraint pruning
  - **-2.1%**: Low - healthy progress

### Status Indicators
- 🟢 **Running**: Actively solving
- 🔴 **Stuck**: No progress in 10+ minutes
- ✅ **Solved**: Completed (depth >= totalPieces)
- 🔵 **Milestone**: Best depth saved
- ⚪ **Idle**: No recent activity

## 🏆 Best Practices

1. **Monitor the Delta**: High delta early indicates unpromising configuration
2. **Track Speed**: Decreasing pieces/sec suggests approaching bottleneck
3. **Use Time Ranges**: Compare hourly vs daily trends for insights
4. **Watch Top 5**: Focus resources on high-performing configurations
5. **Check Historical**: Plateaus in chart indicate potential dead-ends

## 🛠️ Development

### Project Structure
```
eternity/
├── src/main/java/
│   ├── monitoring/
│   │   ├── config/          # WebSocket, CORS config
│   │   ├── controller/      # REST & WebSocket endpoints
│   │   ├── model/           # ConfigMetrics, HistoricalMetrics
│   │   ├── repository/      # JPA repository
│   │   ├── service/         # FileWatcher, Parser, Aggregator
│   │   └── MonitoringApplication.java
│   └── MainParallel.java    # Solver entry point
├── frontend/
│   ├── src/
│   │   ├── components/      # React components
│   │   ├── services/        # API & WebSocket clients
│   │   ├── types/           # TypeScript interfaces
│   │   └── App.tsx
│   ├── package.json
│   └── vite.config.ts
├── saves/                   # Save files directory
├── data/                    # H2 database
└── pom.xml                  # Maven dependencies
```

### Adding New Metrics

1. **Backend**: Update `ConfigMetrics.java`
2. **Parser**: Add regex pattern to `SaveFileParser.java`
3. **Frontend**: Update `metrics.ts` TypeScript interface
4. **UI**: Add display in `ConfigTable.tsx` or `ConfigDetailsModal.tsx`

### Running Tests
```bash
mvn test
```

### Building for Production
```bash
# Backend
mvn clean package -DskipTests

# Frontend
cd frontend
npm run build  # Outputs to src/main/resources/static/

# Run as single JAR
java -jar target/eternity-solver-1.0.0-monitoring.jar
# Access at http://localhost:8080 (serves both backend + frontend)
```

## 📝 License

This monitoring system is part of the Eternity Puzzle Solver project.

## 🙏 Credits

- **Backend**: Spring Boot, H2 Database, WebSocket (STOMP)
- **Frontend**: React, TypeScript, Vite, TailwindCSS, Chart.js
- **Real-time**: SockJS, STOMP.js

---

**Last Updated**: December 2025
**Version**: 1.0.0
