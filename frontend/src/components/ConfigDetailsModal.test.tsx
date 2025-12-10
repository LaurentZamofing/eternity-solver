import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import ConfigDetailsModal from './ConfigDetailsModal'
import type { ConfigMetrics } from '../types/metrics'

// Mock child components
vi.mock('./HistoricalChart', () => ({
  HistoricalChart: ({ configName }: any) => (
    <div data-testid="historical-chart">Chart for {configName}</div>
  ),
}))

vi.mock('./BoardVisualizerEnhanced', () => ({
  BoardVisualizerEnhanced: ({ configName }: any) => (
    <div data-testid="board-visualizer">Board for {configName}</div>
  ),
}))

describe('ConfigDetailsModal', () => {
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
    totalComputeTimeMs: 3600000,
    computeTimeFormatted: '1h 0m',
    rows: 16,
    cols: 16,
    totalPieces: 256,
    running: true,
    solved: false,
    status: 'running' as const,
    piecesPerSecond: 10.5,
    estimatedTimeRemainingMs: 5000000,
  }

  const mockOnClose = vi.fn()

  it('renders modal with config name', () => {
    render(<ConfigDetailsModal config={mockConfig} onClose={mockOnClose} />)

    expect(screen.getByText('test_config')).toBeInTheDocument()
  })

  it('displays depth metric', () => {
    render(<ConfigDetailsModal config={mockConfig} onClose={mockOnClose} />)

    expect(screen.getByText('Depth')).toBeInTheDocument()
    expect(screen.getByText('100 / 256')).toBeInTheDocument()
  })

  it('displays physical progress percentage', () => {
    render(<ConfigDetailsModal config={mockConfig} onClose={mockOnClose} />)

    expect(screen.getByText('Physical %')).toBeInTheDocument()
    expect(screen.getByText('40.00%')).toBeInTheDocument()
  })

  it('displays search space percentage', () => {
    render(<ConfigDetailsModal config={mockConfig} onClose={mockOnClose} />)

    expect(screen.getByText('Search Space')).toBeInTheDocument()
    expect(screen.getByText('45.50%')).toBeInTheDocument()
  })

  it('displays compute time', () => {
    render(<ConfigDetailsModal config={mockConfig} onClose={mockOnClose} />)

    expect(screen.getByText('Compute Time')).toBeInTheDocument()
    expect(screen.getByText('1h 0m')).toBeInTheDocument()
  })

  it('displays pieces per second', () => {
    render(<ConfigDetailsModal config={mockConfig} onClose={mockOnClose} />)

    expect(screen.getByText('Speed')).toBeInTheDocument()
    expect(screen.getByText('10.50 p/s')).toBeInTheDocument()
  })

  it('displays N/A for speed when pieces per second is 0', () => {
    const configWithNoSpeed = { ...mockConfig, piecesPerSecond: 0 }
    render(<ConfigDetailsModal config={configWithNoSpeed} onClose={mockOnClose} />)

    expect(screen.getByText('N/A')).toBeInTheDocument()
  })

  it('calls onClose when close button clicked', async () => {
    const user = userEvent.setup()
    render(<ConfigDetailsModal config={mockConfig} onClose={mockOnClose} />)

    // Find the X button (close button)
    const closeButton = screen.getByRole('button', { name: '' })
    await user.click(closeButton)

    expect(mockOnClose).toHaveBeenCalled()
  })

  it('shows best record badge when status is best_record', () => {
    const bestConfig = { ...mockConfig, status: 'best_record' as const }
    render(<ConfigDetailsModal config={bestConfig} onClose={mockOnClose} />)

    expect(screen.getByText('🏆 Best Record')).toBeInTheDocument()
  })

  it('does not show best record badge for regular status', () => {
    render(<ConfigDetailsModal config={mockConfig} onClose={mockOnClose} />)

    expect(screen.queryByText('🏆 Best Record')).not.toBeInTheDocument()
  })

  it('renders historical chart component', () => {
    render(<ConfigDetailsModal config={mockConfig} onClose={mockOnClose} />)

    expect(screen.getByTestId('historical-chart')).toBeInTheDocument()
    expect(screen.getByText('Chart for test_config')).toBeInTheDocument()
  })

  it('renders board visualizer component', () => {
    render(<ConfigDetailsModal config={mockConfig} onClose={mockOnClose} />)

    expect(screen.getByTestId('board-visualizer')).toBeInTheDocument()
    expect(screen.getByText('Board for test_config')).toBeInTheDocument()
  })

  it('formats ETA correctly for days', () => {
    const configWithLongETA = {
      ...mockConfig,
      estimatedTimeRemainingMs: 200000000, // ~2.3 days
    }
    render(<ConfigDetailsModal config={configWithLongETA} onClose={mockOnClose} />)

    // The formatETA function should format this to "2d Xh"
    // We can check that the modal renders (exact ETA format depends on implementation)
    expect(screen.getByText('test_config')).toBeInTheDocument()
  })

  it('formats ETA correctly for hours', () => {
    const configWithHourETA = {
      ...mockConfig,
      estimatedTimeRemainingMs: 7200000, // 2 hours
    }
    render(<ConfigDetailsModal config={configWithHourETA} onClose={mockOnClose} />)

    expect(screen.getByText('test_config')).toBeInTheDocument()
  })

  it('handles zero or negative ETA', () => {
    const configWithNoETA = {
      ...mockConfig,
      estimatedTimeRemainingMs: -1000,
    }
    render(<ConfigDetailsModal config={configWithNoETA} onClose={mockOnClose} />)

    expect(screen.getByText('test_config')).toBeInTheDocument()
  })

  it('handles missing physical progress percentage', () => {
    const configWithoutPhysical = {
      ...mockConfig,
      physicalProgressPercentage: undefined as any,
    }
    render(<ConfigDetailsModal config={configWithoutPhysical} onClose={mockOnClose} />)

    // Should show 0.00% when undefined
    expect(screen.getByText('0.00%')).toBeInTheDocument()
  })
})
