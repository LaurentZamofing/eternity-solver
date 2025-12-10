import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { HistoricalChart } from './HistoricalChart'
import { apiService } from '../services/api'
import type { HistoricalMetrics } from '../types/metrics'

// Mock the apiService
vi.mock('../services/api', () => ({
  apiService: {
    getConfigHistory: vi.fn(),
  },
}))

describe('HistoricalChart', () => {
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
    {
      id: 2,
      configName: 'test_config',
      timestamp: '2025-12-10T11:00:00',
      depth: 60,
      progressPercentage: 55.2,
      totalComputeTimeMs: 2000,
      status: 'running',
      piecesPerSecond: 12.3,
    },
    {
      id: 3,
      configName: 'test_config',
      timestamp: '2025-12-10T12:00:00',
      depth: 70,
      progressPercentage: 65.8,
      totalComputeTimeMs: 3000,
      status: 'running',
      piecesPerSecond: 15.1,
    },
  ]

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('shows loading state initially', () => {
    vi.mocked(apiService.getConfigHistory).mockImplementation(
      () => new Promise(() => {}) // Never resolves
    )

    render(<HistoricalChart configName="test_config" />)

    expect(screen.getByText('Loading history...')).toBeInTheDocument()
  })

  it('renders chart with historical data', async () => {
    vi.mocked(apiService.getConfigHistory).mockResolvedValue(mockHistory)

    render(<HistoricalChart configName="test_config" />)

    await waitFor(() => {
      expect(screen.getByText('Progress History')).toBeInTheDocument()
    })

    expect(screen.getByText('Max Depth')).toBeInTheDocument()
    expect(screen.getByText('70')).toBeInTheDocument()
    expect(screen.getByText('Max Progress')).toBeInTheDocument()
    expect(screen.getByText('65.80%')).toBeInTheDocument()
  })

  it('displays stats summary correctly', async () => {
    vi.mocked(apiService.getConfigHistory).mockResolvedValue(mockHistory)

    render(<HistoricalChart configName="test_config" />)

    await waitFor(() => {
      expect(screen.getByText('Max Depth')).toBeInTheDocument()
    })

    // Max depth should be 70
    expect(screen.getByText('70')).toBeInTheDocument()

    // Max progress should be 65.80%
    expect(screen.getByText('65.80%')).toBeInTheDocument()

    // Average speed: (10.5 + 12.3 + 15.1) / 3 = 12.63 p/s
    expect(screen.getByText('12.63 p/s')).toBeInTheDocument()

    // Data points: 3
    expect(screen.getByText('3')).toBeInTheDocument()
  })

  it('shows error state when API call fails', async () => {
    vi.mocked(apiService.getConfigHistory).mockRejectedValue(
      new Error('Network error')
    )

    render(<HistoricalChart configName="test_config" />)

    await waitFor(() => {
      expect(screen.getByText('Failed to load historical data')).toBeInTheDocument()
    })
  })

  it('shows empty state when no data', async () => {
    vi.mocked(apiService.getConfigHistory).mockResolvedValue([])

    render(<HistoricalChart configName="test_config" />)

    await waitFor(() => {
      expect(screen.getByText('No historical data available yet.')).toBeInTheDocument()
    })

    expect(
      screen.getByText('Data will appear as the solver makes progress.')
    ).toBeInTheDocument()
  })

  it('allows changing time range', async () => {
    vi.mocked(apiService.getConfigHistory).mockResolvedValue(mockHistory)
    const user = userEvent.setup()

    render(<HistoricalChart configName="test_config" hours={24} />)

    await waitFor(() => {
      expect(screen.getByText('Progress History')).toBeInTheDocument()
    })

    const timeRangeSelect = screen.getByLabelText('Time Range:')
    await user.selectOptions(timeRangeSelect, '6')

    await waitFor(() => {
      expect(apiService.getConfigHistory).toHaveBeenCalledWith('test_config', 6)
    })
  })

  it('renders time range selector with all options', async () => {
    vi.mocked(apiService.getConfigHistory).mockResolvedValue(mockHistory)

    render(<HistoricalChart configName="test_config" />)

    await waitFor(() => {
      expect(screen.getByText('Progress History')).toBeInTheDocument()
    })

    const select = screen.getByLabelText('Time Range:')
    expect(select).toBeInTheDocument()

    expect(screen.getByRole('option', { name: 'Last hour' })).toBeInTheDocument()
    expect(screen.getByRole('option', { name: 'Last 6 hours' })).toBeInTheDocument()
    expect(screen.getByRole('option', { name: 'Last 24 hours' })).toBeInTheDocument()
    expect(screen.getByRole('option', { name: 'Last 3 days' })).toBeInTheDocument()
    expect(screen.getByRole('option', { name: 'Last week' })).toBeInTheDocument()
  })

  it('uses default hours value of 24', async () => {
    vi.mocked(apiService.getConfigHistory).mockResolvedValue(mockHistory)

    render(<HistoricalChart configName="test_config" />)

    await waitFor(() => {
      expect(apiService.getConfigHistory).toHaveBeenCalledWith('test_config', 24)
    })
  })

  it('uses provided hours value', async () => {
    vi.mocked(apiService.getConfigHistory).mockResolvedValue(mockHistory)

    render(<HistoricalChart configName="test_config" hours={6} />)

    await waitFor(() => {
      expect(apiService.getConfigHistory).toHaveBeenCalledWith('test_config', 6)
    })
  })

  it('renders canvas element for chart', async () => {
    vi.mocked(apiService.getConfigHistory).mockResolvedValue(mockHistory)

    const { container } = render(<HistoricalChart configName="test_config" />)

    await waitFor(() => {
      expect(screen.getByText('Progress History')).toBeInTheDocument()
    })

    expect(container.querySelector('canvas')).toBeInTheDocument()
  })
})
