import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { ConfigMetrics, GlobalStats } from './types/metrics'

// Use vi.hoisted to define mocks before vi.mock calls
const {
  mockGetAllConfigs,
  mockGetGlobalStats,
  mockRefreshCache,
  mockConnect,
  mockDisconnect,
  mockOnConnect,
  mockOnDisconnect,
  mockOnError,
  mockOnMetricsUpdate
} = vi.hoisted(() => ({
  mockGetAllConfigs: vi.fn(),
  mockGetGlobalStats: vi.fn(),
  mockRefreshCache: vi.fn(),
  mockConnect: vi.fn(),
  mockDisconnect: vi.fn(),
  mockOnConnect: vi.fn(),
  mockOnDisconnect: vi.fn(),
  mockOnError: vi.fn(),
  mockOnMetricsUpdate: vi.fn(),
}))

vi.mock('./services/api', () => ({
  apiService: {
    getAllConfigs: mockGetAllConfigs,
    getGlobalStats: mockGetGlobalStats,
    refreshCache: mockRefreshCache,
  },
}))

vi.mock('./services/websocket', () => ({
  websocketService: {
    connect: mockConnect,
    disconnect: mockDisconnect,
    onConnect: mockOnConnect,
    onDisconnect: mockOnDisconnect,
    onError: mockOnError,
    onMetricsUpdate: mockOnMetricsUpdate,
  },
}))

// Mock child components for faster tests
vi.mock('./components/GlobalStats', () => ({
  default: ({ stats }: any) => (
    <div data-testid="global-stats">
      Total: {stats.totalConfigs}
    </div>
  ),
}))

vi.mock('./components/ConfigTable', () => ({
  default: ({ configs, sortField, sortOrder, statusFilter, onSortChange, onFilterChange }: any) => (
    <div data-testid="config-table">
      <div>Configs: {configs.length}</div>
      <div>Sort: {sortField} {sortOrder}</div>
      <div>Filter: {statusFilter}</div>
      <button onClick={() => onSortChange('depth', 'asc')}>Change Sort</button>
      <button onClick={() => onFilterChange('running')}>Change Filter</button>
    </div>
  ),
}))

vi.mock('./components/ConnectionStatus', () => ({
  default: ({ connected }: any) => (
    <div data-testid="connection-status">
      {connected ? 'Connected' : 'Disconnected'}
    </div>
  ),
}))

vi.mock('./components/MetricsLegend', () => ({
  MetricsLegend: () => <div data-testid="metrics-legend">Legend</div>,
}))

// Import App after all mocks
import App from './App'

describe('App Integration Tests', () => {
  const mockConfigs: ConfigMetrics[] = [
    {
      configName: 'test_config_1',
      timestamp: Date.now(),
      lastUpdate: '2025-12-10T10:00:00',
      lastSaveDate: '2025-12-10 10:00:00',
      lastSaveRelative: '1m ago',
      depth: 100,
      bestDepthEver: 120,
      progressPercentage: 45.5,
      physicalProgressPercentage: 40.0,
      totalComputeTimeMs: 3600000,
      computeTimeFormatted: '1h 0m',
      rows: 16,
      cols: 16,
      totalPieces: 256,
      running: true,
      solved: false,
      status: 'running',
      piecesPerSecond: 10.5,
      estimatedTimeRemainingMs: 5000000,
    },
    {
      configName: 'test_config_2',
      timestamp: Date.now(),
      lastUpdate: '2025-12-10T10:00:00',
      lastSaveDate: '2025-12-10 10:00:00',
      lastSaveRelative: '2m ago',
      depth: 80,
      bestDepthEver: 80,
      progressPercentage: 35.0,
      physicalProgressPercentage: 30.0,
      totalComputeTimeMs: 2400000,
      computeTimeFormatted: '40m 0s',
      rows: 16,
      cols: 16,
      totalPieces: 256,
      running: false,
      solved: false,
      status: 'idle',
      piecesPerSecond: 8.0,
      estimatedTimeRemainingMs: 6000000,
    },
  ]

  const mockGlobalStats: GlobalStats = {
    totalConfigs: 2,
    runningConfigs: 1,
    solvedConfigs: 0,
    idleConfigs: 1,
    stuckConfigs: 0,
    totalComputeTimeMs: 6000000,
    totalComputeTimeFormatted: '1h 40m',
    bestProgressPercentage: 45.5,
    bestProgressConfigName: 'test_config_1',
    averageProgressPercentage: 40.25,
    maxDepth: 100,
    maxDepthConfigName: 'test_config_1',
    fastestPiecesPerSecond: 10.5,
    fastestConfigName: 'test_config_1',
    topConfigs: [],
    stuckConfigsList: [],
  }

  beforeEach(() => {
    vi.clearAllMocks()

    // Setup default mock behaviors
    mockGetAllConfigs.mockResolvedValue(mockConfigs)
    mockGetGlobalStats.mockResolvedValue(mockGlobalStats)
    mockRefreshCache.mockResolvedValue({ success: true, configsFound: 2 })

    // Mock WebSocket subscriptions to return unsubscribe functions
    mockOnConnect.mockReturnValue(() => {})
    mockOnDisconnect.mockReturnValue(() => {})
    mockOnError.mockReturnValue(() => {})
    mockOnMetricsUpdate.mockReturnValue(() => {})
  })

  afterEach(() => {
    vi.clearAllTimers()
  })

  describe('Initial Loading', () => {
    it('shows loading state initially', () => {
      mockGetAllConfigs.mockImplementation(() => new Promise(() => {}))
      mockGetGlobalStats.mockImplementation(() => new Promise(() => {}))

      render(<App />)

      expect(screen.getByText('Loading monitoring dashboard...')).toBeInTheDocument()
    })

    it('loads and displays initial data', async () => {
      render(<App />)

      await waitFor(() => {
        expect(screen.getByText('Eternity Solver Monitor')).toBeInTheDocument()
      })

      expect(mockGetAllConfigs).toHaveBeenCalledWith('progress', 'desc', 'all')
      expect(mockGetGlobalStats).toHaveBeenCalled()
      expect(screen.getByTestId('global-stats')).toBeInTheDocument()
      expect(screen.getByTestId('config-table')).toBeInTheDocument()
    })

    it('shows error when initial data fails to load', async () => {
      mockGetAllConfigs.mockRejectedValue(new Error('Network error'))
      mockGetGlobalStats.mockRejectedValue(new Error('Network error'))

      render(<App />)

      await waitFor(() => {
        expect(screen.getByText('Failed to load configurations')).toBeInTheDocument()
      })
    })

    it('connects to WebSocket on mount', async () => {
      render(<App />)

      await waitFor(() => {
        expect(mockConnect).toHaveBeenCalled()
      })

      expect(mockOnConnect).toHaveBeenCalled()
      expect(mockOnDisconnect).toHaveBeenCalled()
      expect(mockOnError).toHaveBeenCalled()
      expect(mockOnMetricsUpdate).toHaveBeenCalled()
    })
  })

  describe('WebSocket Integration', () => {
    it('updates connection status when connected', async () => {
      let connectCallback: (() => void) | null = null
      mockOnConnect.mockImplementation((cb) => {
        connectCallback = cb
        return () => {}
      })

      render(<App />)

      await waitFor(() => {
        expect(screen.getByText('Eternity Solver Monitor')).toBeInTheDocument()
      })

      // Initially disconnected
      expect(screen.getByTestId('connection-status')).toHaveTextContent('Disconnected')

      // Trigger connection
      if (connectCallback) {
        connectCallback()
      }

      await waitFor(() => {
        expect(screen.getByTestId('connection-status')).toHaveTextContent('Connected')
      })
    })

    it('handles WebSocket errors', async () => {
      let errorCallback: ((err: any) => void) | null = null
      mockOnError.mockImplementation((cb) => {
        errorCallback = cb
        return () => {}
      })

      render(<App />)

      await waitFor(() => {
        expect(screen.getByText('Eternity Solver Monitor')).toBeInTheDocument()
      })

      // Trigger error
      if (errorCallback) {
        errorCallback(new Error('Connection failed'))
      }

      await waitFor(() => {
        expect(screen.getByText('WebSocket connection error')).toBeInTheDocument()
      })
    })

    it('updates config when receiving WebSocket metrics', async () => {
      let metricsCallback: ((metrics: ConfigMetrics) => void) | null = null
      mockOnMetricsUpdate.mockImplementation((cb) => {
        metricsCallback = cb
        return () => {}
      })

      render(<App />)

      await waitFor(() => {
        expect(screen.getByText('Configs: 2')).toBeInTheDocument()
      })

      // Send updated metrics via WebSocket
      const updatedMetrics: ConfigMetrics = {
        ...mockConfigs[0],
        depth: 110,
        progressPercentage: 50.0,
      }

      if (metricsCallback) {
        metricsCallback(updatedMetrics)
      }

      // Should reload global stats after metrics update
      await waitFor(() => {
        expect(mockGetGlobalStats).toHaveBeenCalledTimes(2) // Initial + after update
      })
    })

    it('disconnects WebSocket on unmount', async () => {
      const { unmount } = render(<App />)

      await waitFor(() => {
        expect(screen.getByText('Eternity Solver Monitor')).toBeInTheDocument()
      })

      unmount()

      expect(mockDisconnect).toHaveBeenCalled()
    })
  })

  describe('Sorting and Filtering', () => {
    it('reloads configs when sort changes', async () => {
      const user = userEvent.setup()
      render(<App />)

      await waitFor(() => {
        expect(screen.getByText('Eternity Solver Monitor')).toBeInTheDocument()
      })

      // Initial load
      expect(mockGetAllConfigs).toHaveBeenCalledWith('progress', 'desc', 'all')

      // Change sort
      const changeSortButton = screen.getByText('Change Sort')
      await user.click(changeSortButton)

      await waitFor(() => {
        expect(mockGetAllConfigs).toHaveBeenCalledWith('depth', 'asc', 'all')
      })
    })

    it('reloads configs when filter changes', async () => {
      const user = userEvent.setup()
      render(<App />)

      await waitFor(() => {
        expect(screen.getByText('Eternity Solver Monitor')).toBeInTheDocument()
      })

      // Initial load
      expect(mockGetAllConfigs).toHaveBeenCalledWith('progress', 'desc', 'all')

      // Change filter
      const changeFilterButton = screen.getByText('Change Filter')
      await user.click(changeFilterButton)

      await waitFor(() => {
        expect(mockGetAllConfigs).toHaveBeenCalledWith('progress', 'desc', 'running')
      })
    })
  })

  describe('Cache Refresh', () => {
    it('refreshes cache and reloads data', async () => {
      const user = userEvent.setup()

      // Make refresh take longer to observe loading state
      let resolveRefresh: any
      mockRefreshCache.mockImplementation(() =>
        new Promise(resolve => {
          resolveRefresh = () => resolve({ success: true, configsFound: 2 })
        })
      )

      render(<App />)

      await waitFor(() => {
        expect(screen.getByText('Refresh')).toBeInTheDocument()
      })

      // Click refresh button
      const refreshButton = screen.getByText('Refresh')
      await user.click(refreshButton)

      // Should show refreshing state immediately
      expect(screen.getByText('Refreshing...')).toBeInTheDocument()
      expect(mockRefreshCache).toHaveBeenCalled()

      // Resolve the refresh
      resolveRefresh()

      // Should reload data after refresh
      await waitFor(() => {
        expect(mockGetAllConfigs).toHaveBeenCalledTimes(2) // Initial + after refresh
        expect(mockGetGlobalStats).toHaveBeenCalledTimes(2) // Initial + after refresh
      })

      // Should go back to normal state
      await waitFor(() => {
        expect(screen.getByText('Refresh')).toBeInTheDocument()
      })
    })

    it('handles refresh errors', async () => {
      const user = userEvent.setup()
      mockRefreshCache.mockRejectedValue(new Error('Refresh failed'))

      render(<App />)

      await waitFor(() => {
        expect(screen.getByText('Refresh')).toBeInTheDocument()
      })

      const refreshButton = screen.getByText('Refresh')
      await user.click(refreshButton)

      await waitFor(() => {
        expect(screen.getByText('Failed to refresh cache')).toBeInTheDocument()
      })
    })

    it('disables refresh button while refreshing', async () => {
      const user = userEvent.setup()

      // Make refresh take longer to observe disabled state
      let resolveRefresh: any
      mockRefreshCache.mockImplementation(() =>
        new Promise(resolve => {
          resolveRefresh = () => resolve({ success: true })
        })
      )

      render(<App />)

      await waitFor(() => {
        expect(screen.getByText('Refresh')).toBeInTheDocument()
      })

      const refreshButton = screen.getByText('Refresh') as HTMLButtonElement
      await user.click(refreshButton)

      // Button should show "Refreshing..." and be disabled
      const refreshingButton = screen.getByText('Refreshing...') as HTMLButtonElement
      expect(refreshingButton).toBeDisabled()

      // Resolve the refresh
      resolveRefresh()

      // Wait for button to go back to normal
      await waitFor(() => {
        const normalButton = screen.getByText('Refresh') as HTMLButtonElement
        expect(normalButton).not.toBeDisabled()
      })
    })
  })

  describe('Component Integration', () => {
    it('renders all main components', async () => {
      render(<App />)

      await waitFor(() => {
        expect(screen.getByText('Eternity Solver Monitor')).toBeInTheDocument()
      })

      expect(screen.getByTestId('global-stats')).toBeInTheDocument()
      expect(screen.getByTestId('metrics-legend')).toBeInTheDocument()
      expect(screen.getByTestId('config-table')).toBeInTheDocument()
      expect(screen.getByTestId('connection-status')).toBeInTheDocument()
    })

    it('displays correct config count in header', async () => {
      render(<App />)

      await waitFor(() => {
        expect(screen.getByText('Real-time monitoring of 2 configurations')).toBeInTheDocument()
      })
    })

    it('passes correct props to ConfigTable', async () => {
      render(<App />)

      await waitFor(() => {
        expect(screen.getByText('Configs: 2')).toBeInTheDocument()
        expect(screen.getByText('Sort: progress desc')).toBeInTheDocument()
        expect(screen.getByText('Filter: all')).toBeInTheDocument()
      })
    })

    it('passes correct stats to GlobalStats', async () => {
      render(<App />)

      await waitFor(() => {
        expect(screen.getByText('Total: 2')).toBeInTheDocument()
      })
    })
  })

  describe('Periodic Updates', () => {
    it('reloads global stats periodically', async () => {
      vi.useFakeTimers()

      render(<App />)

      await waitFor(() => {
        expect(screen.getByText('Eternity Solver Monitor')).toBeInTheDocument()
      })

      // Initial call
      expect(mockGetGlobalStats).toHaveBeenCalledTimes(1)

      // Fast-forward 10 seconds and run all timers
      vi.advanceTimersByTime(10000)
      await vi.runAllTimersAsync()

      await waitFor(() => {
        expect(mockGetGlobalStats).toHaveBeenCalledTimes(2)
      })

      // Fast-forward another 10 seconds
      vi.advanceTimersByTime(10000)
      await vi.runAllTimersAsync()

      await waitFor(() => {
        expect(mockGetGlobalStats).toHaveBeenCalledTimes(3)
      })

      vi.useRealTimers()
    }, 10000) // Increase timeout for fake timers test
  })
})
