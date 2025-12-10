import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { CellDetailsModal } from './CellDetailsModal'
import type { CellDetails } from '../types/metrics'

// Mock fetch
global.fetch = vi.fn()

// Mock PieceVisualizer
vi.mock('./PieceVisualizer', () => ({
  PieceVisualizer: ({ pieceId, rotation }: any) => (
    <div data-testid="piece-visualizer">
      Piece {pieceId} R{rotation}
    </div>
  ),
}))

describe('CellDetailsModal', () => {
  const mockCellDetails: CellDetails = {
    position: { row: 5, col: 10 },
    currentPiece: {
      pieceId: 42,
      rotation: 1,
      edges: [1, 2, 3, 4],
      valid: true,
      current: true,
      alreadyTried: false,
    },
    possiblePieces: [
      {
        pieceId: 10,
        rotation: 0,
        edges: [5, 6, 7, 8],
        valid: true,
        current: false,
        alreadyTried: false,
      },
      {
        pieceId: 20,
        rotation: 2,
        edges: [9, 10, 11, 12],
        valid: false,
        current: false,
        alreadyTried: true,
        invalidReason: 'Edge mismatch',
      },
      {
        pieceId: 30,
        rotation: 1,
        edges: [13, 14, 15, 16],
        valid: true,
        current: false,
        alreadyTried: false,
      },
    ],
    constraints: {
      borders: {
        north: true,
        east: false,
        south: false,
        west: false,
      },
      neighbors: {
        south: {
          row: 6,
          col: 10,
          pieceId: 100,
          rotation: 0,
          edgeValue: 5,
        },
      },
    },
    statistics: {
      totalPieces: 256,
      usedPieces: 50,
      availablePieces: 206,
      validOptions: 2,
      invalidOptions: 1,
    },
  }

  const mockOnClose = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(fetch).mockResolvedValue({
      ok: true,
      json: async () => mockCellDetails,
    } as Response)
  })

  it('shows loading state initially', () => {
    vi.mocked(fetch).mockImplementation(
      () => new Promise(() => {}) // Never resolves
    )

    render(
      <CellDetailsModal
        configName="test_config"
        row={5}
        col={10}
        onClose={mockOnClose}
      />
    )

    expect(screen.getByText('Loading cell details...')).toBeInTheDocument()
  })

  it('renders cell details after loading', async () => {
    render(
      <CellDetailsModal
        configName="test_config"
        row={5}
        col={10}
        onClose={mockOnClose}
      />
    )

    await waitFor(() => {
      expect(screen.getByText('Cell Details')).toBeInTheDocument()
    })

    expect(screen.getByText('test_config - Position [5, 10]')).toBeInTheDocument()
  })

  it('displays statistics correctly', async () => {
    render(
      <CellDetailsModal
        configName="test_config"
        row={5}
        col={10}
        onClose={mockOnClose}
      />
    )

    await waitFor(() => {
      expect(screen.getByText('Total Pieces')).toBeInTheDocument()
    })

    expect(screen.getByText('256')).toBeInTheDocument()
    expect(screen.getByText('Used')).toBeInTheDocument()
    expect(screen.getByText('50')).toBeInTheDocument()
    expect(screen.getByText('Available')).toBeInTheDocument()
    expect(screen.getByText('206')).toBeInTheDocument()
    expect(screen.getByText('Valid Options')).toBeInTheDocument()
    expect(screen.getByText('2')).toBeInTheDocument()
    expect(screen.getByText('Invalid')).toBeInTheDocument()
    expect(screen.getByText('1')).toBeInTheDocument()
  })

  it('displays current piece information', async () => {
    render(
      <CellDetailsModal
        configName="test_config"
        row={5}
        col={10}
        onClose={mockOnClose}
      />
    )

    await waitFor(() => {
      expect(screen.getByText('Current Piece')).toBeInTheDocument()
    })

    expect(screen.getByText('Piece ID: 42')).toBeInTheDocument()
    expect(screen.getByText('Rotation: 1 (×90°)')).toBeInTheDocument()
    expect(screen.getByText('Edges: [1, 2, 3, 4]')).toBeInTheDocument()
  })

  it('shows error state when API call fails', async () => {
    vi.mocked(fetch).mockResolvedValue({
      ok: false,
      statusText: 'Not Found',
    } as Response)

    render(
      <CellDetailsModal
        configName="test_config"
        row={5}
        col={10}
        onClose={mockOnClose}
      />
    )

    await waitFor(() => {
      expect(screen.getByText(/Failed to load cell details/)).toBeInTheDocument()
    })
  })

  it('calls onClose when close button clicked', async () => {
    const user = userEvent.setup()

    render(
      <CellDetailsModal
        configName="test_config"
        row={5}
        col={10}
        onClose={mockOnClose}
      />
    )

    await waitFor(() => {
      expect(screen.getByText('Cell Details')).toBeInTheDocument()
    })

    // Find the X button (close button)
    const closeButton = screen.getByRole('button', { name: '' })
    await user.click(closeButton)

    expect(mockOnClose).toHaveBeenCalled()
  })

  it('filters pieces by "valid" mode', async () => {
    const user = userEvent.setup()

    render(
      <CellDetailsModal
        configName="test_config"
        row={5}
        col={10}
        onClose={mockOnClose}
      />
    )

    await waitFor(() => {
      expect(screen.getByText('Cell Details')).toBeInTheDocument()
    })

    // Click "Valid" filter button
    const validButton = screen.getByText(/Valid \(2\)/)
    await user.click(validButton)

    // Should show 2 valid pieces (excluding current piece)
    const pieceVisualizers = screen.getAllByTestId('piece-visualizer')
    // Current piece + 2 valid options = 3 total in view
    // But the "Possible Pieces" section excludes current, so 2
  })

  it('toggles view mode between current and historical', async () => {
    const user = userEvent.setup()

    render(
      <CellDetailsModal
        configName="test_config"
        row={5}
        col={10}
        onClose={mockOnClose}
      />
    )

    await waitFor(() => {
      expect(screen.getByText('View Mode')).toBeInTheDocument()
    })

    const historicalButton = screen.getByText('Historical (At Time of Placement)')
    await user.click(historicalButton)

    await waitFor(() => {
      expect(fetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/configs/test_config/cell/5/10/details/historical'
      )
    })
  })

  it('displays constraints correctly', async () => {
    render(
      <CellDetailsModal
        configName="test_config"
        row={5}
        col={10}
        onClose={mockOnClose}
      />
    )

    await waitFor(() => {
      expect(screen.getByText('Constraints')).toBeInTheDocument()
    })

    expect(screen.getByText('Borders:')).toBeInTheDocument()
    expect(screen.getByText('north')).toBeInTheDocument()

    expect(screen.getByText('Neighbors:')).toBeInTheDocument()
    expect(screen.getByText(/south: Piece 100 r0 \(edge: 5\)/)).toBeInTheDocument()
  })

  it('shows "No borders" when cell has no borders', async () => {
    const noBorderDetails = {
      ...mockCellDetails,
      constraints: {
        borders: {
          north: false,
          east: false,
          south: false,
          west: false,
        },
        neighbors: {},
      },
    }

    vi.mocked(fetch).mockResolvedValue({
      ok: true,
      json: async () => noBorderDetails,
    } as Response)

    render(
      <CellDetailsModal
        configName="test_config"
        row={5}
        col={10}
        onClose={mockOnClose}
      />
    )

    await waitFor(() => {
      expect(screen.getByText('No borders')).toBeInTheDocument()
    })
  })

  it('shows "No neighbors" when cell has no neighbors', async () => {
    const noNeighborDetails = {
      ...mockCellDetails,
      constraints: {
        borders: {
          north: true,
          east: false,
          south: false,
          west: false,
        },
        neighbors: {},
      },
    }

    vi.mocked(fetch).mockResolvedValue({
      ok: true,
      json: async () => noNeighborDetails,
    } as Response)

    render(
      <CellDetailsModal
        configName="test_config"
        row={5}
        col={10}
        onClose={mockOnClose}
      />
    )

    await waitFor(() => {
      expect(screen.getByText('No neighbors')).toBeInTheDocument()
    })
  })

  it('shows filter counts correctly', async () => {
    render(
      <CellDetailsModal
        configName="test_config"
        row={5}
        col={10}
        onClose={mockOnClose}
      />
    )

    await waitFor(() => {
      expect(screen.getByText('Cell Details')).toBeInTheDocument()
    })

    // Total includes current piece + 3 possible pieces = 4
    expect(screen.getByText(/All \(4\)/)).toBeInTheDocument()
    // Valid options from statistics
    expect(screen.getByText(/Valid \(2\)/)).toBeInTheDocument()
    // Already tried: 1
    expect(screen.getByText(/Already Tried \(1\)/)).toBeInTheDocument()
  })
})
