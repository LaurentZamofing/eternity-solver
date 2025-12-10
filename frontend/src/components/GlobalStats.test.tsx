import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import GlobalStatsComponent from './GlobalStats'
import type { GlobalStats } from '../types/metrics'

describe('GlobalStatsComponent', () => {
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

  it('renders system status cards', () => {
    render(<GlobalStatsComponent stats={mockStats} />)

    expect(screen.getByText('System Status')).toBeInTheDocument()
    expect(screen.getByText('Total Configs')).toBeInTheDocument()
    expect(screen.getByText('10')).toBeInTheDocument()
    expect(screen.getByText('Running')).toBeInTheDocument()
    expect(screen.getByText('3')).toBeInTheDocument()
    expect(screen.getByText('Solved')).toBeInTheDocument()
    expect(screen.getByText('2')).toBeInTheDocument()
    expect(screen.getByText('Stuck')).toBeInTheDocument()
    expect(screen.getByText('1')).toBeInTheDocument()
  })

  it('renders performance metrics cards', () => {
    render(<GlobalStatsComponent stats={mockStats} />)

    expect(screen.getByText('Performance Metrics')).toBeInTheDocument()
    expect(screen.getByText('Best Progress')).toBeInTheDocument()
    expect(screen.getByText('85.50%')).toBeInTheDocument()
    expect(screen.getByText('config_best')).toBeInTheDocument()

    expect(screen.getByText('Average Progress')).toBeInTheDocument()
    expect(screen.getByText('45.20%')).toBeInTheDocument()

    expect(screen.getByText('Max Depth')).toBeInTheDocument()
    expect(screen.getByText('120')).toBeInTheDocument()
    expect(screen.getByText('config_deep')).toBeInTheDocument()

    expect(screen.getByText('Total Time')).toBeInTheDocument()
    expect(screen.getByText('1h 0m')).toBeInTheDocument()
  })

  it('renders top performers when available', () => {
    const statsWithTop: GlobalStats = {
      ...mockStats,
      topConfigs: [
        {
          configName: 'config_1',
          depth: 100,
          progressPercentage: 90.5,
          timestamp: Date.now(),
          lastUpdate: '2025-12-10T10:00:00',
          lastSaveDate: '2025-12-10 10:00:00',
          lastSaveRelative: '1m ago',
          bestDepthEver: 100,
          physicalProgressPercentage: 85.0,
          totalComputeTimeMs: 1000,
          computeTimeFormatted: '1s',
          rows: 16,
          cols: 16,
          totalPieces: 256,
          running: true,
          solved: false,
          status: 'running' as const,
          piecesPerSecond: 10,
          estimatedTimeRemainingMs: 5000,
        },
        {
          configName: 'config_2',
          depth: 95,
          progressPercentage: 88.2,
          timestamp: Date.now(),
          lastUpdate: '2025-12-10T10:00:00',
          lastSaveDate: '2025-12-10 10:00:00',
          lastSaveRelative: '2m ago',
          bestDepthEver: 95,
          physicalProgressPercentage: 82.0,
          totalComputeTimeMs: 2000,
          computeTimeFormatted: '2s',
          rows: 16,
          cols: 16,
          totalPieces: 256,
          running: false,
          solved: false,
          status: 'idle' as const,
          piecesPerSecond: 8,
          estimatedTimeRemainingMs: 6000,
        },
      ],
    }

    render(<GlobalStatsComponent stats={statsWithTop} />)

    expect(screen.getByText('🏆 Top 5 Performers')).toBeInTheDocument()
    expect(screen.getByText('config_1')).toBeInTheDocument()
    expect(screen.getByText('Depth: 100')).toBeInTheDocument()
    expect(screen.getByText('90.50%')).toBeInTheDocument()
    expect(screen.getByText('config_2')).toBeInTheDocument()
    expect(screen.getByText('Depth: 95')).toBeInTheDocument()
    expect(screen.getByText('88.20%')).toBeInTheDocument()
  })

  it('does not render top performers when empty', () => {
    render(<GlobalStatsComponent stats={mockStats} />)

    expect(screen.queryByText('🏆 Top 5 Performers')).not.toBeInTheDocument()
  })

  it('limits top performers to 5 entries', () => {
    const topConfigs = Array.from({ length: 10 }, (_, i) => ({
      configName: `config_${i}`,
      depth: 100 - i,
      progressPercentage: 90 - i,
      timestamp: Date.now(),
      lastUpdate: '2025-12-10T10:00:00',
      lastSaveDate: '2025-12-10 10:00:00',
      lastSaveRelative: '1m ago',
      bestDepthEver: 100 - i,
      physicalProgressPercentage: 85.0,
      totalComputeTimeMs: 1000,
      computeTimeFormatted: '1s',
      rows: 16,
      cols: 16,
      totalPieces: 256,
      running: true,
      solved: false,
      status: 'running' as const,
      piecesPerSecond: 10,
      estimatedTimeRemainingMs: 5000,
    }))

    const statsWithMany: GlobalStats = {
      ...mockStats,
      topConfigs,
    }

    render(<GlobalStatsComponent stats={statsWithMany} />)

    // Should only show 5
    expect(screen.getByText('config_0')).toBeInTheDocument()
    expect(screen.getByText('config_4')).toBeInTheDocument()
    expect(screen.queryByText('config_5')).not.toBeInTheDocument()
  })
})
