import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import ConfigTable from './ConfigTable'
import type { ConfigMetrics } from '../types/metrics'

// Mock the ConfigDetailsModal component
vi.mock('./ConfigDetailsModal', () => ({
  default: ({ config, onClose }: any) => (
    <div data-testid="config-details-modal">
      <span>Config: {config.configName}</span>
      <button onClick={onClose}>Close Modal</button>
    </div>
  ),
}))

describe('ConfigTable', () => {
  const mockConfigs: ConfigMetrics[] = [
    {
      configName: 'config_1',
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
      status: 'running' as const,
      piecesPerSecond: 10.5,
      estimatedTimeRemainingMs: 5000,
    },
    {
      configName: 'config_2',
      timestamp: Date.now(),
      lastUpdate: '2025-12-10T10:00:00',
      lastSaveDate: '2025-12-10 10:00:00',
      lastSaveRelative: '2m ago',
      depth: 95,
      bestDepthEver: 110,
      progressPercentage: 42.0,
      physicalProgressPercentage: 38.0,
      totalComputeTimeMs: 2000,
      computeTimeFormatted: '2s',
      rows: 16,
      cols: 16,
      totalPieces: 256,
      running: false,
      solved: false,
      status: 'idle' as const,
      piecesPerSecond: 8.2,
      estimatedTimeRemainingMs: 6000,
    },
  ]

  const mockCallbacks = {
    onSortChange: vi.fn(),
    onFilterChange: vi.fn(),
  }

  it('renders table with configuration data', () => {
    render(
      <ConfigTable
        configs={mockConfigs}
        sortField="name"
        sortOrder="asc"
        statusFilter="all"
        {...mockCallbacks}
      />
    )

    expect(screen.getByText('Configuration Status (2)')).toBeInTheDocument()
    expect(screen.getByText('config_1')).toBeInTheDocument()
    expect(screen.getByText('config_2')).toBeInTheDocument()
  })

  it('displays depth information correctly', () => {
    render(
      <ConfigTable
        configs={mockConfigs}
        sortField="name"
        sortOrder="asc"
        statusFilter="all"
        {...mockCallbacks}
      />
    )

    expect(screen.getByText('100 / 256')).toBeInTheDocument()
    expect(screen.getByText('95 / 256')).toBeInTheDocument()
  })

  it('shows best depth with backtrack indicator', () => {
    render(
      <ConfigTable
        configs={mockConfigs}
        sortField="name"
        sortOrder="asc"
        statusFilter="all"
        {...mockCallbacks}
      />
    )

    // config_2 has bestDepthEver (110) > depth (95), should show backtrack
    expect(screen.getByText('(-15)')).toBeInTheDocument()
  })

  it('renders status badges correctly', () => {
    render(
      <ConfigTable
        configs={mockConfigs}
        sortField="name"
        sortOrder="asc"
        statusFilter="all"
        {...mockCallbacks}
      />
    )

    expect(screen.getByText('Running')).toBeInTheDocument()
    expect(screen.getByText('Idle')).toBeInTheDocument()
  })

  it('handles filter change', async () => {
    const user = userEvent.setup()
    render(
      <ConfigTable
        configs={mockConfigs}
        sortField="name"
        sortOrder="asc"
        statusFilter="all"
        {...mockCallbacks}
      />
    )

    const filterSelect = screen.getByRole('combobox')
    await user.selectOptions(filterSelect, 'running')

    expect(mockCallbacks.onFilterChange).toHaveBeenCalledWith('running')
  })

  it('handles sort by name', async () => {
    const user = userEvent.setup()
    render(
      <ConfigTable
        configs={mockConfigs}
        sortField="name"
        sortOrder="asc"
        statusFilter="all"
        {...mockCallbacks}
      />
    )

    const nameHeader = screen.getByText(/Configuration/).closest('th')
    await user.click(nameHeader!)

    expect(mockCallbacks.onSortChange).toHaveBeenCalledWith('name', 'desc')
  })

  it('handles sort by depth', async () => {
    const user = userEvent.setup()
    render(
      <ConfigTable
        configs={mockConfigs}
        sortField="progress"
        sortOrder="desc"
        statusFilter="all"
        {...mockCallbacks}
      />
    )

    const depthHeader = screen.getByText(/Depth/).closest('th')
    await user.click(depthHeader!)

    expect(mockCallbacks.onSortChange).toHaveBeenCalledWith('depth', 'desc')
  })

  it('toggles sort order when clicking same field', async () => {
    const user = userEvent.setup()
    render(
      <ConfigTable
        configs={mockConfigs}
        sortField="depth"
        sortOrder="desc"
        statusFilter="all"
        {...mockCallbacks}
      />
    )

    const depthHeader = screen.getByText(/Depth/).closest('th')
    await user.click(depthHeader!)

    expect(mockCallbacks.onSortChange).toHaveBeenCalledWith('depth', 'asc')
  })

  it('shows empty state when no configs', () => {
    render(
      <ConfigTable
        configs={[]}
        sortField="name"
        sortOrder="asc"
        statusFilter="all"
        {...mockCallbacks}
      />
    )

    expect(screen.getByText('No configurations found.')).toBeInTheDocument()
  })

  it('opens modal when clicking View Details', async () => {
    const user = userEvent.setup()
    render(
      <ConfigTable
        configs={mockConfigs}
        sortField="name"
        sortOrder="asc"
        statusFilter="all"
        {...mockCallbacks}
      />
    )

    const viewButtons = screen.getAllByText('View Details')
    await user.click(viewButtons[0])

    expect(screen.getByTestId('config-details-modal')).toBeInTheDocument()
    expect(screen.getByText('Config: config_1')).toBeInTheDocument()
  })

  it('shows View Best button when bestDepthEver > depth', () => {
    render(
      <ConfigTable
        configs={mockConfigs}
        sortField="name"
        sortOrder="asc"
        statusFilter="all"
        {...mockCallbacks}
      />
    )

    // config_2 has bestDepthEver (110) > depth (95)
    const viewBestButtons = screen.getAllByText('View Best')
    expect(viewBestButtons).toHaveLength(1)
  })

  it('calculates and displays delta correctly', () => {
    render(
      <ConfigTable
        configs={mockConfigs}
        sortField="name"
        sortOrder="asc"
        statusFilter="all"
        {...mockCallbacks}
      />
    )

    // config_1: progressPercentage (45.5) - physicalProgressPercentage (40.0) = +5.5%
    expect(screen.getByText('+5.5%')).toBeInTheDocument()
    // config_2: progressPercentage (42.0) - physicalProgressPercentage (38.0) = +4.0%
    expect(screen.getByText('+4.0%')).toBeInTheDocument()
  })

  it('displays formatted compute time', () => {
    render(
      <ConfigTable
        configs={mockConfigs}
        sortField="name"
        sortOrder="asc"
        statusFilter="all"
        {...mockCallbacks}
      />
    )

    expect(screen.getByText('1s')).toBeInTheDocument()
    expect(screen.getByText('2s')).toBeInTheDocument()
  })

  it('displays pieces per second', () => {
    render(
      <ConfigTable
        configs={mockConfigs}
        sortField="name"
        sortOrder="asc"
        statusFilter="all"
        {...mockCallbacks}
      />
    )

    expect(screen.getByText('10.50')).toBeInTheDocument()
    expect(screen.getByText('8.20')).toBeInTheDocument()
  })
})
