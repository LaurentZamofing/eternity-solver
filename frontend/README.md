# Eternity Monitoring Dashboard

Real-time monitoring dashboard for the Eternity puzzle solver. Built with React, TypeScript, and Vite.

## 🎯 Overview

This dashboard provides real-time visualization and monitoring of the Eternity puzzle solver's progress:
- **Live metrics** via WebSocket (depth, progress, compute time)
- **Configuration comparison** across multiple solver runs
- **Historical data** with interactive charts
- **Board visualization** with piece placement details
- **Performance analytics** and statistics

## 🏗️ Architecture

### Tech Stack

- **Framework:** React 18 with TypeScript
- **Build Tool:** Vite 5 (fast HMR, optimized builds)
- **Styling:** Tailwind CSS 3
- **Charts:** Chart.js 4 with React wrapper
- **WebSocket:** STOMP over SockJS
- **HTTP Client:** Axios
- **Testing:** Vitest + React Testing Library

### Project Structure

```
frontend/
├── src/
│   ├── components/          # React components
│   │   ├── App.tsx          # Main app with routing
│   │   ├── BoardVisualizer.tsx      # Grid visualization
│   │   ├── ConfigTable.tsx          # Configuration list
│   │   ├── PieceVisualizer.tsx      # Individual piece
│   │   ├── CellDetailsModal.tsx     # Cell details popup
│   │   ├── MetricsPanel.tsx         # Real-time metrics
│   │   ├── StatsPanel.tsx           # Statistics display
│   │   └── HistoricalChart.tsx      # Progress charts
│   ├── services/            # API and WebSocket services
│   │   ├── api.ts           # REST API client
│   │   └── websocket.ts     # WebSocket connection
│   ├── types/               # TypeScript type definitions
│   │   └── index.ts         # Shared types
│   ├── test/                # Test utilities
│   │   └── setup.ts         # Test configuration
│   └── main.tsx             # App entry point
├── public/                  # Static assets
├── vitest.config.ts         # Test configuration
├── vite.config.ts           # Vite configuration
├── tailwind.config.js       # Tailwind CSS config
└── package.json             # Dependencies and scripts
```

## 🚀 Getting Started

### Prerequisites

- Node.js 20.x or higher
- npm 10.x or higher
- Backend server running on `http://localhost:8080`

### Installation

```bash
# Install dependencies
npm install

# Start development server
npm run dev

# Open browser at http://localhost:5173
```

### Available Scripts

```bash
# Development
npm run dev              # Start dev server with HMR
npm run build            # Build for production
npm run preview          # Preview production build

# Code Quality
npm run lint             # Run ESLint

# Testing
npm test                 # Run tests in watch mode
npm run test:ui          # Open Vitest UI
npm run test:coverage    # Generate coverage report
```

## 🧪 Testing

### Test Infrastructure

Tests are configured with **Vitest** and **React Testing Library**:
- Fast execution with native ESM support
- Component testing with user interaction simulation
- Coverage reporting (target: 70%+)
- Interactive UI for debugging

### Running Tests

```bash
# Watch mode (recommended during development)
npm test

# Single run (CI/CD)
npm test -- --run

# With UI
npm run test:ui

# With coverage
npm run test:coverage
```

### Writing Tests

Example component test:

```typescript
import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import PieceVisualizer from './PieceVisualizer'

describe('PieceVisualizer', () => {
  it('renders piece with label', () => {
    render(
      <PieceVisualizer
        pieceId={42}
        north={1}
        east={2}
        south={3}
        west={4}
        rotation={0}
        showLabel={true}
      />
    )

    expect(screen.getByText('42')).toBeInTheDocument()
  })
})
```

## 📡 API Integration

### REST API

Base URL: `http://localhost:8080`

**Endpoints:**
- `GET /api/configs` - List all configurations
  - Query params: `sortBy`, `order`, `status`
- `GET /api/configs/{name}` - Get specific config
- `GET /api/configs/{name}/cell/{row}/{col}` - Cell details
- `GET /api/configs/{name}/history` - Historical metrics
- `GET /api/global-stats` - Global statistics

### WebSocket

Connection: `ws://localhost:8080/ws-monitoring`

**Topics:**
- `/topic/metrics` - Real-time config metrics
- `/topic/stats` - Global statistics updates

**Subscribe example:**
```typescript
import { subscribe } from './services/websocket'

subscribe('/topic/metrics', (metrics) => {
  console.log('New metrics:', metrics)
})
```

## 🎨 Components

### Main Components

#### App.tsx
Main application component with state management and WebSocket integration.

**Responsibilities:**
- WebSocket connection lifecycle
- Global state management
- Routing and navigation
- Error handling

#### BoardVisualizer.tsx
Displays the puzzle grid with piece placements.

**Props:**
- `board: number[][]` - Board state (piece IDs)
- `pieces: Piece[]` - Available pieces
- `size?: number` - Cell size in pixels

**Features:**
- Interactive cell selection
- Rotation visualization
- Empty cell indicators
- Click handlers for details

#### ConfigTable.tsx
Lists all solver configurations with sorting and filtering.

**Features:**
- Sort by: name, progress, depth, speed
- Filter by status
- Real-time updates via WebSocket
- Click to view details

#### PieceVisualizer.tsx
Renders an individual puzzle piece with edge patterns.

**Props:**
- `pieceId: number` - Piece identifier
- `north/east/south/west: number` - Edge pattern IDs
- `rotation: number` - Rotation angle (0, 90, 180, 270)
- `showLabel: boolean` - Display piece ID

### Chart Components

#### HistoricalChart.tsx
Interactive chart showing solver progress over time.

**Features:**
- Time-series visualization
- Multiple metrics (depth, progress %)
- Zoom and pan
- Responsive design

## 🎯 State Management

Currently uses React hooks for local state:
- `useState` for component state
- `useEffect` for side effects
- Custom hooks for WebSocket subscriptions

Future: Consider Zustand or Jotai for complex global state.

## 🔧 Configuration

### Vite Config

Key configurations in `vite.config.ts`:
- React plugin with Fast Refresh
- Proxy to backend (`/api`, `/ws-monitoring`, `/patterns`, `/pieces`)
- Build optimizations

### Proxy Setup

Development proxy (avoids CORS issues):
```typescript
proxy: {
  '/api': 'http://localhost:8080',
  '/ws-monitoring': {
    target: 'ws://localhost:8080',
    ws: true
  },
  '/patterns': 'http://localhost:8080',
  '/pieces': 'http://localhost:8080'
}
```

## 🎨 Styling

### Tailwind CSS

Utility-first CSS framework for rapid UI development.

**Common patterns:**
```tsx
// Card layout
<div className="bg-white rounded-lg shadow-md p-6">

// Grid layout
<div className="grid grid-cols-3 gap-4">

// Responsive
<div className="hidden md:block lg:flex">
```

### Custom Styles

Component-specific styles in `.css` files:
- `PieceVisualizer.css` - Piece rendering
- `BoardVisualizer.css` - Grid layout

## 🐛 Debugging

### Development Tools

1. **React DevTools** - Component inspection
2. **Vite DevTools** - Build analysis
3. **Browser DevTools** - Network, console
4. **Vitest UI** - Test debugging

### Common Issues

**WebSocket not connecting:**
- Check backend is running on port 8080
- Verify proxy configuration in `vite.config.ts`

**Components not updating:**
- Check WebSocket subscriptions
- Verify state updates with React DevTools

**Build errors:**
- Clear `node_modules` and reinstall
- Check TypeScript errors: `npm run build`

## 📝 Code Conventions

### TypeScript

- Use interfaces for props and API responses
- Avoid `any` - use `unknown` if type is truly unknown
- Enable strict mode

### React

- Functional components with hooks
- Props destructuring
- Meaningful component names (PascalCase)

### Files

- One component per file
- Co-locate tests: `Component.test.tsx`
- Co-locate styles: `Component.css`

## 🚀 Deployment

### Production Build

```bash
npm run build
# Output: dist/
```

### Serve Static Files

The `dist/` folder contains:
- Minified JavaScript bundles
- Optimized CSS
- HTML with cache-busting hashes

Serve with:
- Nginx
- Apache
- Spring Boot static resources
- CDN (CloudFront, Cloudflare)

### Environment Variables

Create `.env.production`:
```
VITE_API_URL=https://api.example.com
VITE_WS_URL=wss://api.example.com/ws-monitoring
```

## 🤝 Contributing

### Adding Components

1. Create component: `src/components/MyComponent.tsx`
2. Add tests: `src/components/MyComponent.test.tsx`
3. Export from `components/` if reusable
4. Document props with TypeScript interfaces

### Adding API Endpoints

1. Update `services/api.ts`
2. Add TypeScript types to `types/index.ts`
3. Handle errors appropriately
4. Add tests

## 📚 Resources

- [React Docs](https://react.dev)
- [Vite Docs](https://vitejs.dev)
- [Vitest Docs](https://vitest.dev)
- [Tailwind CSS](https://tailwindcss.com)
- [Chart.js](https://www.chartjs.org)

## 📄 License

Part of the Eternity Solver project.
