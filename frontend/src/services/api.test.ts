import { describe, it, expect, vi, beforeEach } from 'vitest'
import type { ConfigMetrics, GlobalStats, HistoricalMetrics } from '../types/metrics'

// Mock axios before importing the service
const mockGet = vi.fn()
const mockPost = vi.fn()

vi.mock('axios', () => ({
  default: {
    create: vi.fn(() => ({
      get: mockGet,
      post: mockPost,
    })),
  },
}))

// Import service after mocking
import { apiService } from './api'

describe('apiService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getAllConfigs', () => {
    it('fetches all configs with default parameters', async () => {
      const mockConfigs: ConfigMetrics[] = [
        {
          configName: 'test_config',
          timestamp: Date.now(),
          lastUpdate: '2025-12-10T10:00:00',
          lastSaveDate: '2025-12-10 10:00:00',
          lastSaveRelative: '1m ago',
          depth: 100,
          bestDepthEver: 100,
          progressPercentage: 45.5,
          physicalProgressPercentage: 40.0,
          totalComputeTimeMs: 1000,
          computeTimeFormatted: '1s',
          rows: 16,
          cols: 16,
          totalPieces: 256,
          running: true,
          solved: false,
          status: 'running',
          piecesPerSecond: 10.5,
          estimatedTimeRemainingMs: 5000,
        },
      ]

      mockGet.mockResolvedValue({ data: mockConfigs })

      const result = await apiService.getAllConfigs()

      expect(mockGet).toHaveBeenCalledWith('/configs', {
        params: { sort: 'progress', order: 'desc' },
      })
      expect(result).toEqual(mockConfigs)
    })

    it('fetches configs with custom sort and order', async () => {
      const mockConfigs: ConfigMetrics[] = []
      mockGet.mockResolvedValue({ data: mockConfigs })

      await apiService.getAllConfigs('depth', 'asc')

      expect(mockGet).toHaveBeenCalledWith('/configs', {
        params: { sort: 'depth', order: 'asc' },
      })
    })

    it('fetches configs with status filter', async () => {
      const mockConfigs: ConfigMetrics[] = []
      mockGet.mockResolvedValue({ data: mockConfigs })

      await apiService.getAllConfigs('progress', 'desc', 'running')

      expect(mockGet).toHaveBeenCalledWith('/configs', {
        params: { sort: 'progress', order: 'desc', status: 'running' },
      })
    })

    it('excludes status filter when set to "all"', async () => {
      const mockConfigs: ConfigMetrics[] = []
      mockGet.mockResolvedValue({ data: mockConfigs })

      await apiService.getAllConfigs('progress', 'desc', 'all')

      expect(mockGet).toHaveBeenCalledWith('/configs', {
        params: { sort: 'progress', order: 'desc' },
      })
    })
  })

  describe('getConfig', () => {
    it('fetches specific config by name', async () => {
      const mockConfig: ConfigMetrics = {
        configName: 'test_config',
        timestamp: Date.now(),
        lastUpdate: '2025-12-10T10:00:00',
        lastSaveDate: '2025-12-10 10:00:00',
        lastSaveRelative: '1m ago',
        depth: 100,
        bestDepthEver: 100,
        progressPercentage: 45.5,
        physicalProgressPercentage: 40.0,
        totalComputeTimeMs: 1000,
        computeTimeFormatted: '1s',
        rows: 16,
        cols: 16,
        totalPieces: 256,
        running: true,
        solved: false,
        status: 'running',
        piecesPerSecond: 10.5,
        estimatedTimeRemainingMs: 5000,
      }

      mockGet.mockResolvedValue({ data: mockConfig })

      const result = await apiService.getConfig('test_config')

      expect(mockGet).toHaveBeenCalledWith('/configs/test_config')
      expect(result).toEqual(mockConfig)
    })
  })

  describe('getConfigHistory', () => {
    it('fetches config history with default parameters', async () => {
      const mockHistory: HistoricalMetrics[] = [
        {
          id: 1,
          configName: 'test_config',
          timestamp: '2025-12-10T10:00:00',
          depth: 50,
          progressPercentage: 45.5,
          totalComputeTimeMs: 1000,
          status: 'running',
          piecesPerSecond: 10.5,
        },
      ]

      mockGet.mockResolvedValue({ data: mockHistory })

      const result = await apiService.getConfigHistory('test_config')

      expect(mockAxiosInstance.get).toHaveBeenCalledWith(
        '/configs/test_config/history',
        {
          params: { hours: 24, limit: 1000 },
        }
      )
      expect(result).toEqual(mockHistory)
    })

    it('fetches config history with custom parameters', async () => {
      const mockHistory: HistoricalMetrics[] = []
      mockGet.mockResolvedValue({ data: mockHistory })

      await apiService.getConfigHistory('test_config', 6, 500)

      expect(mockAxiosInstance.get).toHaveBeenCalledWith(
        '/configs/test_config/history',
        {
          params: { hours: 6, limit: 500 },
        }
      )
    })
  })

  describe('getGlobalStats', () => {
    it('fetches global statistics', async () => {
      const mockStats: GlobalStats = {
        totalConfigs: 10,
        runningConfigs: 3,
        solvedConfigs: 2,
        idleConfigs: 4,
        stuckConfigs: 1,
        totalComputeTimeMs: 3600000,
        totalComputeTimeFormatted: '1h 0m',
        bestProgressPercentage: 85.5,
        bestProgressConfigName: 'config_best',
        averageProgressPercentage: 45.2,
        maxDepth: 120,
        maxDepthConfigName: 'config_deep',
        fastestPiecesPerSecond: 150.5,
        fastestConfigName: 'config_fast',
        topConfigs: [],
        stuckConfigsList: [],
      }

      mockGet.mockResolvedValue({ data: mockStats })

      const result = await apiService.getGlobalStats()

      expect(mockAxiosInstance.get).toHaveBeenCalledWith('/stats/global')
      expect(result).toEqual(mockStats)
    })
  })

  describe('getRecentActivity', () => {
    it('fetches recent activity with default hours', async () => {
      const mockActivity: HistoricalMetrics[] = []
      mockGet.mockResolvedValue({ data: mockActivity })

      await apiService.getRecentActivity()

      expect(mockAxiosInstance.get).toHaveBeenCalledWith('/stats/recent', {
        params: { hours: 1 },
      })
    })

    it('fetches recent activity with custom hours', async () => {
      const mockActivity: HistoricalMetrics[] = []
      mockGet.mockResolvedValue({ data: mockActivity })

      await apiService.getRecentActivity(6)

      expect(mockAxiosInstance.get).toHaveBeenCalledWith('/stats/recent', {
        params: { hours: 6 },
      })
    })
  })

  describe('healthCheck', () => {
    it('performs health check', async () => {
      const mockHealth = {
        status: 'UP',
        timestamp: '2025-12-10T10:00:00',
        configsMonitored: 10,
        databaseRecords: 1000,
      }

      mockGet.mockResolvedValue({ data: mockHealth })

      const result = await apiService.healthCheck()

      expect(mockAxiosInstance.get).toHaveBeenCalledWith('/health')
      expect(result).toEqual(mockHealth)
    })
  })

  describe('getConfigsSummary', () => {
    it('fetches configs summary', async () => {
      const mockSummary = {
        totalConfigs: 10,
        configs: [
          { name: 'config1', depth: 100, progress: 45.5, status: 'running' },
          { name: 'config2', depth: 95, progress: 42.0, status: 'idle' },
        ],
      }

      mockGet.mockResolvedValue({ data: mockSummary })

      const result = await apiService.getConfigsSummary()

      expect(mockAxiosInstance.get).toHaveBeenCalledWith('/configs/summary')
      expect(result).toEqual(mockSummary)
    })
  })

  describe('refreshCache', () => {
    it('refreshes cache', async () => {
      const mockResponse = {
        success: true,
        message: 'Cache refreshed',
        configsFound: 10,
        timestamp: '2025-12-10T10:00:00',
      }

      mockPost.mockResolvedValue({ data: mockResponse })

      const result = await apiService.refreshCache()

      expect(mockAxiosInstance.post).toHaveBeenCalledWith('/refresh')
      expect(result).toEqual(mockResponse)
    })
  })

  describe('error handling', () => {
    it('propagates axios errors', async () => {
      const error = new Error('Network error')
      mockGet.mockRejectedValue(error)

      await expect(apiService.getAllConfigs()).rejects.toThrow('Network error')
    })

    it('handles 404 errors', async () => {
      const error = { response: { status: 404, data: 'Not found' } }
      mockGet.mockRejectedValue(error)

      await expect(apiService.getConfig('nonexistent')).rejects.toEqual(error)
    })
  })
})
