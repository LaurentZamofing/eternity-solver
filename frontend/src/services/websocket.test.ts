import { describe, it, expect, vi, beforeEach } from 'vitest'
import { WebSocketService } from './websocket'
import type { ConfigMetrics, GlobalStats } from '../types/metrics'

// Mock @stomp/stompjs
const mockSubscribe = vi.fn()
const mockPublish = vi.fn()
const mockActivate = vi.fn()
const mockDeactivate = vi.fn()

class MockClient {
  subscribe = mockSubscribe
  publish = mockPublish
  activate = mockActivate
  deactivate = mockDeactivate
  onConnect: any = null
  onDisconnect: any = null
  onStompError: any = null
  onWebSocketError: any = null

  constructor() {}
}

vi.mock('@stomp/stompjs', () => ({
  Client: MockClient,
}))

// Mock sockjs-client
vi.mock('sockjs-client', () => ({
  default: vi.fn().mockImplementation(() => ({})),
}))

describe('WebSocketService', () => {
  let service: WebSocketService

  beforeEach(() => {
    vi.clearAllMocks()
    service = new WebSocketService()
  })

  describe('connect', () => {
    it('creates and activates STOMP client', () => {
      service.connect()

      expect(mockActivate).toHaveBeenCalled()
    })

    it('does not connect if already connected', () => {
      service.connect()
      // Simulate connection
      const client = (service as any).client
      if (client && client.onConnect) {
        client.onConnect()
      }

      mockActivate.mockClear()
      service.connect()

      expect(mockActivate).not.toHaveBeenCalled()
    })

    it('sets up connection callbacks', () => {
      service.connect()

      const client = (service as any).client
      expect(client.onConnect).toBeDefined()
      expect(client.onDisconnect).toBeDefined()
      expect(client.onStompError).toBeDefined()
      expect(client.onWebSocketError).toBeDefined()
    })

    it('calls connect callbacks on successful connection', () => {
      const callback = vi.fn()
      service.onConnect(callback)

      service.connect()
      const client = (service as any).client
      if (client && client.onConnect) {
        client.onConnect()
      }

      expect(callback).toHaveBeenCalled()
    })
  })

  describe('disconnect', () => {
    it('deactivates client when connected', () => {
      service.connect()
      service.disconnect()

      expect(mockDeactivate).toHaveBeenCalled()
    })

    it('sets connected to false', () => {
      service.connect()
      const client = (service as any).client
      if (client && client.onConnect) {
        client.onConnect()
      }

      expect(service.isConnected()).toBe(true)

      service.disconnect()
      expect(service.isConnected()).toBe(false)
    })

    it('calls disconnect callbacks', () => {
      const callback = vi.fn()
      service.onDisconnect(callback)

      service.connect()
      const client = (service as any).client
      if (client && client.onConnect) {
        client.onConnect()
      }

      if (client && client.onDisconnect) {
        client.onDisconnect()
      }

      expect(callback).toHaveBeenCalled()
    })
  })

  describe('onMetricsUpdate', () => {
    it('registers metrics callback', () => {
      const callback = vi.fn()
      const mockMetrics: ConfigMetrics = {
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

      service.onMetricsUpdate(callback)

      // Simulate receiving a metrics message
      service.connect()
      const client = (service as any).client
      if (client && client.onConnect) {
        client.onConnect()
      }

      // Find the subscription callback for /topic/metrics
      const subscribeCall = mockSubscribe.mock.calls.find(
        (call) => call[0] === '/topic/metrics'
      )
      if (subscribeCall) {
        const messageCallback = subscribeCall[1]
        messageCallback({ body: JSON.stringify(mockMetrics) })
      }

      expect(callback).toHaveBeenCalledWith(mockMetrics)
    })

    it('returns unsubscribe function', () => {
      const callback = vi.fn()
      const unsubscribe = service.onMetricsUpdate(callback)

      expect(typeof unsubscribe).toBe('function')

      unsubscribe()

      // Callback should not be called after unsubscribe
      service.connect()
      const client = (service as any).client
      if (client && client.onConnect) {
        client.onConnect()
      }

      const subscribeCall = mockSubscribe.mock.calls.find(
        (call) => call[0] === '/topic/metrics'
      )
      if (subscribeCall) {
        const messageCallback = subscribeCall[1]
        messageCallback({ body: JSON.stringify({}) })
      }

      expect(callback).not.toHaveBeenCalled()
    })
  })

  describe('onStatsUpdate', () => {
    it('registers stats callback', () => {
      const callback = vi.fn()
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

      service.onStatsUpdate(callback)

      service.connect()
      const client = (service as any).client
      if (client && client.onConnect) {
        client.onConnect()
      }

      const subscribeCall = mockSubscribe.mock.calls.find(
        (call) => call[0] === '/topic/stats'
      )
      if (subscribeCall) {
        const messageCallback = subscribeCall[1]
        messageCallback({ body: JSON.stringify(mockStats) })
      }

      expect(callback).toHaveBeenCalledWith(mockStats)
    })
  })

  describe('send', () => {
    it('publishes message when connected', () => {
      service.connect()
      const client = (service as any).client
      if (client && client.onConnect) {
        client.onConnect()
      }

      const message = { test: 'data' }
      service.send('/app/test', message)

      expect(mockPublish).toHaveBeenCalledWith({
        destination: '/app/test',
        body: JSON.stringify(message),
      })
    })

    it('does not send when not connected', () => {
      const message = { test: 'data' }
      service.send('/app/test', message)

      expect(mockPublish).not.toHaveBeenCalled()
    })
  })

  describe('requestConfig', () => {
    it('sends config request message', () => {
      service.connect()
      const client = (service as any).client
      if (client && client.onConnect) {
        client.onConnect()
      }

      service.requestConfig('test_config')

      expect(mockPublish).toHaveBeenCalledWith({
        destination: '/app/getConfig',
        body: JSON.stringify({ configName: 'test_config' }),
      })
    })
  })

  describe('ping', () => {
    it('sends ping message', () => {
      service.connect()
      const client = (service as any).client
      if (client && client.onConnect) {
        client.onConnect()
      }

      service.ping()

      expect(mockPublish).toHaveBeenCalledWith({
        destination: '/app/ping',
        body: JSON.stringify({ type: 'ping' }),
      })
    })
  })

  describe('error handling', () => {
    it('calls error callbacks on STOMP error', () => {
      const callback = vi.fn()
      service.onError(callback)

      service.connect()
      const client = (service as any).client
      if (client && client.onStompError) {
        const error = { message: 'STOMP error' }
        client.onStompError(error)
      }

      expect(callback).toHaveBeenCalledWith({ message: 'STOMP error' })
    })

    it('calls error callbacks on WebSocket error', () => {
      const callback = vi.fn()
      service.onError(callback)

      service.connect()
      const client = (service as any).client
      if (client && client.onWebSocketError) {
        const error = new Error('WebSocket error')
        client.onWebSocketError(error)
      }

      expect(callback).toHaveBeenCalledWith(expect.any(Error))
    })

    it('handles malformed JSON in metrics message', () => {
      const callback = vi.fn()
      service.onMetricsUpdate(callback)

      service.connect()
      const client = (service as any).client
      if (client && client.onConnect) {
        client.onConnect()
      }

      const subscribeCall = mockSubscribe.mock.calls.find(
        (call) => call[0] === '/topic/metrics'
      )
      if (subscribeCall) {
        const messageCallback = subscribeCall[1]
        // Send invalid JSON
        messageCallback({ body: 'invalid json' })
      }

      // Should not call callback with invalid data
      expect(callback).not.toHaveBeenCalled()
    })
  })

  describe('isConnected', () => {
    it('returns false when not connected', () => {
      expect(service.isConnected()).toBe(false)
    })

    it('returns true when connected', () => {
      service.connect()
      const client = (service as any).client
      if (client && client.onConnect) {
        client.onConnect()
      }

      expect(service.isConnected()).toBe(true)
    })

    it('returns false after disconnect', () => {
      service.connect()
      const client = (service as any).client
      if (client && client.onConnect) {
        client.onConnect()
      }

      expect(service.isConnected()).toBe(true)

      service.disconnect()
      expect(service.isConnected()).toBe(false)
    })
  })

  describe('multiple callbacks', () => {
    it('calls multiple metrics callbacks', () => {
      const callback1 = vi.fn()
      const callback2 = vi.fn()
      const mockMetrics: ConfigMetrics = {
        configName: 'test',
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

      service.onMetricsUpdate(callback1)
      service.onMetricsUpdate(callback2)

      service.connect()
      const client = (service as any).client
      if (client && client.onConnect) {
        client.onConnect()
      }

      const subscribeCall = mockSubscribe.mock.calls.find(
        (call) => call[0] === '/topic/metrics'
      )
      if (subscribeCall) {
        const messageCallback = subscribeCall[1]
        messageCallback({ body: JSON.stringify(mockMetrics) })
      }

      expect(callback1).toHaveBeenCalledWith(mockMetrics)
      expect(callback2).toHaveBeenCalledWith(mockMetrics)
    })
  })
})
