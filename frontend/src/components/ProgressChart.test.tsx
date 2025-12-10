import { describe, it, expect } from 'vitest'
import { render } from '@testing-library/react'
import ProgressChart from './ProgressChart'
import type { HistoricalMetrics } from '../types/metrics'

describe('ProgressChart', () => {
  const mockHistory: HistoricalMetrics[] = [
    {
      id: 1,
      configName: 'test_config',
      timestamp: '2025-12-10T10:00:00',
      depth: 50,
      progressPercentage: 45.5,
      totalComputeTimeMs: 1000,
      status: 'running',
      piecesPerSecond: 10,
    },
    {
      id: 2,
      configName: 'test_config',
      timestamp: '2025-12-10T10:01:00',
      depth: 55,
      progressPercentage: 50.2,
      totalComputeTimeMs: 2000,
      status: 'running',
      piecesPerSecond: 12,
    },
    {
      id: 3,
      configName: 'test_config',
      timestamp: '2025-12-10T10:02:00',
      depth: 60,
      progressPercentage: 55.8,
      totalComputeTimeMs: 3000,
      status: 'running',
      piecesPerSecond: 15,
    },
  ]

  it('renders chart container', () => {
    const { container } = render(<ProgressChart history={mockHistory} />)

    expect(container.querySelector('canvas')).toBeInTheDocument()
  })

  it('renders with empty history', () => {
    const { container } = render(<ProgressChart history={[]} />)

    expect(container.querySelector('canvas')).toBeInTheDocument()
  })

  it('applies correct height classes', () => {
    const { container } = render(<ProgressChart history={mockHistory} />)

    const chartContainer = container.firstChild as HTMLElement
    expect(chartContainer).toHaveClass('h-64')
    expect(chartContainer).toHaveClass('md:h-96')
  })

  it('renders with single data point', () => {
    const singleHistory: HistoricalMetrics[] = [mockHistory[0]]
    const { container } = render(<ProgressChart history={singleHistory} />)

    expect(container.querySelector('canvas')).toBeInTheDocument()
  })

  it('handles large dataset', () => {
    const largeHistory: HistoricalMetrics[] = Array.from({ length: 100 }, (_, i) => ({
      id: i,
      configName: 'test_config',
      timestamp: new Date(2025, 11, 10, 10, i).toISOString(),
      depth: 50 + i,
      progressPercentage: 45 + i * 0.5,
      totalComputeTimeMs: 1000 * (i + 1),
      status: 'running',
      piecesPerSecond: 10 + i * 0.1,
    }))

    const { container } = render(<ProgressChart history={largeHistory} />)

    expect(container.querySelector('canvas')).toBeInTheDocument()
  })
})
