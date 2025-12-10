# Eternity Solver Monitoring API Documentation

Version: 1.0.0
Base URL: `http://localhost:8080/api`

## Table of Contents

1. [Overview](#overview)
2. [REST API Endpoints](#rest-api-endpoints)
3. [WebSocket API](#websocket-api)
4. [Data Models](#data-models)
5. [Error Handling](#error-handling)

## Overview

The Monitoring API provides real-time access to solver metrics, configuration status, historical data, and puzzle piece information. It supports both REST and WebSocket protocols for maximum flexibility.

### Key Features

- **Real-time Updates**: WebSocket support for live metrics
- **Historical Data**: Query historical progression with configurable time ranges
- **Cell Analysis**: Detailed constraint and placement information for each cell
- **Pattern Images**: Serve puzzle patterns for visualization
- **Global Statistics**: Aggregated metrics across all configurations

### Technology Stack

- **Backend**: Spring Boot 3.x
- **WebSocket**: STOMP over SockJS
- **Database**: H2 (in-memory)
- **Frontend**: React + TypeScript + Vite

## REST API Endpoints

### Configuration Endpoints

#### GET /api/configs

List all configurations with current metrics.

**Query Parameters:**
- `sort` (optional): Sort field - `progress`, `depth`, `time`, `name`, `bestdepthever` (default: `progress`)
- `order` (optional): Sort order - `asc`, `desc` (default: `desc`)
- `status` (optional): Filter by status - `running`, `idle`, `solved`, `stuck`

**Response:** `200 OK`
```json
[
  {
    "configName": "eternity2_p01_ascending",
    "timestamp": 1702384800000,
    "lastUpdate": "2025-12-10T10:30:00",
    "depth": 120,
    "bestDepthEver": 150,
    "progressPercentage": 46.88,
    "physicalProgressPercentage": 42.5,
    "totalComputeTimeMs": 3600000,
    "computeTimeFormatted": "1h 0m 0s",
    "rows": 16,
    "cols": 16,
    "totalPieces": 256,
    "running": true,
    "solved": false,
    "status": "running",
    "piecesPerSecond": 12.5,
    "estimatedTimeRemainingMs": 5400000
  }
]
```

#### GET /api/configs/{configName}

Get detailed metrics for a specific configuration.

**Path Parameters:**
- `configName`: Configuration identifier

**Response:** `200 OK` or `404 Not Found`

#### GET /api/configs/{configName}/best

Get the best (highest depth) result ever achieved for a configuration.

**Response:** `200 OK` with ConfigMetrics (status set to `"best_record"`)

#### GET /api/configs/{configName}/history

Get historical progression data for a configuration.

**Query Parameters:**
- `hours` (optional): Hours to look back (default: `24`)
- `limit` (optional): Max data points (default: `1000`)

**Response:** `200 OK`
```json
[
  {
    "id": 12345,
    "configName": "eternity2_p01_ascending",
    "timestamp": "2025-12-10T09:00:00",
    "depth": 100,
    "progressPercentage": 39.06,
    "totalComputeTimeMs": 2400000,
    "status": "running",
    "piecesPerSecond": 11.2
  }
]
```

#### GET /api/configs/summary

Get lightweight summary of all configurations.

**Response:** `200 OK`
```json
{
  "totalConfigs": 5,
  "configs": [
    {
      "name": "config1",
      "depth": 100,
      "progress": 39.06,
      "status": "running"
    }
  ]
}
```

### Cell Details Endpoints

#### GET /api/configs/{configName}/cell/{row}/{col}/details

Get detailed information about a specific cell.

**Path Parameters:**
- `configName`: Configuration identifier
- `row`: Row index (0-based)
- `col`: Column index (0-based)

**Response:** `200 OK`
```json
{
  "row": 5,
  "col": 8,
  "isEmpty": false,
  "currentPiece": {
    "pieceId": 42,
    "rotation": 2
  },
  "placementSequence": 15,
  "constraints": {
    "northEdge": 5,
    "eastEdge": null,
    "southEdge": 12,
    "westEdge": 8
  },
  "possiblePieces": [
    {
      "pieceId": 42,
      "rotation": 2,
      "isValid": true,
      "violatedConstraints": []
    }
  ],
  "totalPossibilities": 1
}
```

#### GET /api/configs/{configName}/cell/{row}/{col}/details/historical

Get historical cell details showing constraints as they were when the piece was placed.

**Response:** Same format as cell details, but excludes constraints from pieces placed after this cell.

### Statistics Endpoints

#### GET /api/stats/global

Get aggregated statistics across all configurations.

**Response:** `200 OK`
```json
{
  "totalConfigs": 5,
  "runningConfigs": 3,
  "solvedConfigs": 0,
  "idleConfigs": 2,
  "stuckConfigs": 0,
  "totalComputeTimeMs": 18000000,
  "totalComputeTimeFormatted": "5h 0m",
  "bestProgressPercentage": 58.59,
  "bestProgressConfigName": "config_best",
  "averageProgressPercentage": 42.18,
  "maxDepth": 150,
  "maxDepthConfigName": "config_deep",
  "fastestPiecesPerSecond": 15.3,
  "fastestConfigName": "config_fast",
  "topConfigs": [...],
  "stuckConfigsList": [...]
}
```

#### GET /api/stats/recent

Get recent activity from last N hours.

**Query Parameters:**
- `hours` (optional): Hours to look back (default: `1`)

**Response:** `200 OK` with array of HistoricalMetrics

### Pattern Endpoints

#### GET /api/patterns/list

List all available edge patterns (0-22).

**Response:** `200 OK`
```json
[
  {
    "patternId": 0,
    "name": "border",
    "imagePath": null,
    "imageUrl": null
  },
  {
    "patternId": 1,
    "name": "Pattern 1",
    "imagePath": "/static/patterns/pattern1.png",
    "imageUrl": "/patterns/pattern1.png"
  }
]
```

#### GET /api/patterns/{patternId}

Get information about a specific pattern.

**Path Parameters:**
- `patternId`: Pattern ID (0-22)

**Response:** `200 OK` or `400 Bad Request`

#### GET /api/patterns/{patternId}/image

Serve pattern image file (PNG).

**Response:** `200 OK` with `image/png` content type

### Piece Definition Endpoints

#### GET /api/pieces/{puzzleName}

Get all piece definitions for a puzzle.

**Path Parameters:**
- `puzzleName`: Puzzle identifier (e.g., `eternity2`)

**Response:** `200 OK`
```json
{
  "1": {
    "pieceId": 1,
    "north": 0,
    "east": 5,
    "south": 12,
    "west": 0
  },
  "2": {
    "pieceId": 2,
    "north": 0,
    "east": 8,
    "south": 15,
    "west": 5
  }
}
```

#### GET /api/pieces/{puzzleName}/{pieceId}

Get a specific piece definition.

**Response:** `200 OK` or `404 Not Found`

### System Endpoints

#### GET /api/health

Health check endpoint.

**Response:** `200 OK`
```json
{
  "status": "UP",
  "timestamp": "2025-12-10T10:30:00",
  "configsMonitored": 5,
  "databaseRecords": 12450
}
```

#### POST /api/refresh

Clear cache and rescan all save files.

**Response:** `200 OK` or `500 Internal Server Error`
```json
{
  "success": true,
  "message": "Cache refreshed successfully",
  "configsFound": 5,
  "timestamp": "2025-12-10T10:30:00"
}
```

## WebSocket API

### Connection

**Endpoint:** `ws://localhost:8080/ws`

**Protocol:** STOMP over SockJS

### Client Setup Example

```javascript
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

const socket = new SockJS('http://localhost:8080/ws');
const stompClient = new Client({
  webSocketFactory: () => socket,
  reconnectDelay: 5000,
  heartbeatIncoming: 4000,
  heartbeatOutgoing: 4000,
});

stompClient.onConnect = () => {
  console.log('Connected to WebSocket');

  // Subscribe to real-time metrics updates
  stompClient.subscribe('/topic/metrics', (message) => {
    const metrics = JSON.parse(message.body);
    console.log('Received metrics:', metrics);
  });

  // Subscribe to global stats
  stompClient.subscribe('/topic/stats', (message) => {
    const stats = JSON.parse(message.body);
    console.log('Received stats:', stats);
  });
};

stompClient.activate();
```

### Subscribe Topics

#### /topic/metrics

Real-time configuration metrics updates.

**Message Format:** ConfigMetrics object (JSON)

**Update Frequency:** When save files are modified (typically every few seconds)

#### /topic/stats

Global statistics updates.

**Message Format:** GlobalStats object (JSON)

**Update Frequency:** Periodic (every 10 seconds or when triggered)

#### /user/queue/metrics (Private)

Subscribe on connection to receive current state immediately.

### Message Endpoints

#### /app/getConfig

Request specific configuration metrics.

**Send:**
```json
{
  "configName": "eternity2_p01_ascending"
}
```

**Response Topic:** `/topic/configResponse`

#### /app/ping

Send keep-alive ping.

**Send:**
```json
{
  "type": "ping"
}
```

**Response Topic:** `/topic/pong`
```json
{
  "type": "pong",
  "timestamp": 1702384800000,
  "configCount": 5
}
```

## Data Models

### ConfigMetrics

Complete metrics for a single configuration.

| Field | Type | Description |
|-------|------|-------------|
| configName | string | Configuration identifier |
| timestamp | long | Unix timestamp (ms) |
| lastUpdate | string | ISO 8601 timestamp |
| lastSaveDate | string | Formatted date string |
| lastSaveRelative | string | Relative time (e.g., "5m ago") |
| depth | int | Current search depth |
| bestDepthEver | int | Maximum depth ever reached |
| progressPercentage | double | Search space coverage (0-100) |
| physicalProgressPercentage | double | Physical board completion (0-100) |
| totalComputeTimeMs | long | Total computation time (ms) |
| computeTimeFormatted | string | Human-readable time |
| rows | int | Board height |
| cols | int | Board width |
| totalPieces | int | Total pieces (rows × cols) |
| running | boolean | Whether solver is active |
| solved | boolean | Whether puzzle is solved |
| status | string | Status: running, idle, solved, stuck, best_record |
| piecesPerSecond | double | Current solving speed |
| estimatedTimeRemainingMs | long | ETA in milliseconds |

### GlobalStats

Aggregated statistics across all configurations.

| Field | Type | Description |
|-------|------|-------------|
| totalConfigs | int | Total configurations |
| runningConfigs | int | Currently active |
| solvedConfigs | int | Successfully solved |
| idleConfigs | int | Not running |
| stuckConfigs | int | No progress recently |
| totalComputeTimeMs | long | Sum of all compute times |
| bestProgressPercentage | double | Highest progress achieved |
| bestProgressConfigName | string | Config with best progress |
| averageProgressPercentage | double | Mean progress |
| maxDepth | int | Deepest search achieved |
| fastestPiecesPerSecond | double | Highest solving speed |

### CellDetails

Detailed analysis of a specific board cell.

| Field | Type | Description |
|-------|------|-------------|
| row | int | Row index (0-based) |
| col | int | Column index (0-based) |
| isEmpty | boolean | Whether cell is empty |
| currentPiece | PlacementInfo | Current piece if placed |
| placementSequence | int | Order in which piece was placed |
| constraints | CellConstraints | Edge constraints |
| possiblePieces | List<PieceOption> | All valid piece placements |
| totalPossibilities | int | Count of valid placements |

## Error Handling

### HTTP Status Codes

- `200 OK`: Request successful
- `400 Bad Request`: Invalid parameters
- `404 Not Found`: Resource not found
- `500 Internal Server Error`: Server error

### Error Response Format

```json
{
  "success": false,
  "message": "Error description",
  "timestamp": "2025-12-10T10:30:00"
}
```

### Common Errors

| Error | Cause | Solution |
|-------|-------|----------|
| Config not found | Invalid configName | Check configuration exists |
| Invalid pattern ID | Pattern ID out of range (0-22) | Use valid pattern ID |
| Cell out of bounds | Row/col exceeds board size | Verify board dimensions |
| No historical data | No save files found | Ensure solver has run |

## Rate Limiting

Currently no rate limiting is enforced. For production:
- REST API: Consider 100 requests/minute per client
- WebSocket: Unlimited subscriptions, server-controlled broadcast rate

## CORS

CORS is enabled for all origins (`*`) for development. Configure appropriately for production.

## Performance Considerations

- **Historical queries**: Use `limit` parameter to avoid large datasets
- **WebSocket**: More efficient than polling for real-time updates
- **Cell details**: Computationally expensive, cache when possible
- **Pattern images**: Served as static files, cached by browsers

## See Also

- [Frontend README](../frontend/README.md) - React dashboard documentation
- [Architecture Overview](./ARCHITECTURE.md) - System design and components
- Source code: `src/main/java/monitoring/controller/`
