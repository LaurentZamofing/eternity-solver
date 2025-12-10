import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { BoardVisualizerEnhanced } from './BoardVisualizerEnhanced'
import type { PieceDefinition } from '../types/metrics'

// Mock fetch
global.fetch = vi.fn()

// Mock child components
vi.mock('./CellDetailsModal', () => ({
  CellDetailsModal: ({ configName, row, col, onClose }: any) => (
    <div data-testid="cell-details-modal">
      <span>Cell Details: {configName} [{row}, {col}]</span>
      <button onClick={onClose}>Close</button>
    </div>
  ),
}))

vi.mock('./PieceVisualizer', () => ({
  PieceVisualizer: ({ pieceId, rotation }: any) => (
    <div data-testid="piece-visualizer">
      Piece {pieceId} R{rotation}
    </div>
  ),
}))

describe('BoardVisualizerEnhanced', () => {
  const mockBoardState = [
    ['1_0', '2_1', null],
    ['3_2', null, '4_3'],
    [null, '5_0', '6_1'],
  ]

  const mockPieceDefinitions: Record<number, PieceDefinition> = {
    1: { pieceId: 1, north: 1, east: 2, south: 3, west: 4 },
    2: { pieceId: 2, north: 5, east: 6, south: 7, west: 8 },
    3: { pieceId: 3, north: 9, east: 10, south: 11, west: 12 },
    4: { pieceId: 4, north: 13, east: 14, south: 15, west: 16 },
    5: { pieceId: 5, north: 17, east: 18, south: 19, west: 20 },
    6: { pieceId: 6, north: 21, east: 1, south: 2, west: 3 },
  }

  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(fetch).mockResolvedValue({
      ok: true,
      json: async () => mockPieceDefinitions,
    } as Response)
  })

  it('shows loading state while fetching piece definitions', () => {
    vi.mocked(fetch).mockImplementation(
      () => new Promise(() => {}) // Never resolves
    )

    render(
      <BoardVisualizerEnhanced
        boardState={mockBoardState}
        rows={3}
        cols={3}
        depth={6}
        puzzleName="eternity2"
        configName="test_config"
      />
    )

    expect(screen.getByText('Loading piece definitions...')).toBeInTheDocument()
  })

  it('renders board after loading piece definitions', async () => {
    render(
      <BoardVisualizerEnhanced
        boardState={mockBoardState}
        rows={3}
        cols={3}
        depth={6}
        puzzleName="eternity2"
        configName="test_config"
      />
    )

    await waitFor(() => {
      expect(screen.getByText('Enhanced Board Visualization')).toBeInTheDocument()
    })
  })

  it('fetches piece definitions for correct puzzle', async () => {
    render(
      <BoardVisualizerEnhanced
        boardState={mockBoardState}
        rows={3}
        cols={3}
        depth={6}
        puzzleName="eternity2"
        configName="test_config"
      />
    )

    await waitFor(() => {
      expect(fetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/pieces/eternity2'
      )
    })
  })

  it('shows empty state when board state is not available', async () => {
    render(
      <BoardVisualizerEnhanced
        boardState={undefined}
        rows={3}
        cols={3}
        depth={0}
        puzzleName="eternity2"
        configName="test_config"
      />
    )

    await waitFor(() => {
      expect(screen.getByText('Board visualization not available')).toBeInTheDocument()
    })
  })

  it('renders zoom control buttons', async () => {
    render(
      <BoardVisualizerEnhanced
        boardState={mockBoardState}
        rows={3}
        cols={3}
        depth={6}
        puzzleName="eternity2"
        configName="test_config"
      />
    )

    await waitFor(() => {
      expect(screen.getByText('Small')).toBeInTheDocument()
    })

    expect(screen.getByText('Medium')).toBeInTheDocument()
    expect(screen.getByText('Large')).toBeInTheDocument()
  })

  it('changes zoom level when clicking zoom buttons', async () => {
    const user = userEvent.setup()

    render(
      <BoardVisualizerEnhanced
        boardState={mockBoardState}
        rows={3}
        cols={3}
        depth={6}
        puzzleName="eternity2"
        configName="test_config"
      />
    )

    await waitFor(() => {
      expect(screen.getByText('Enhanced Board Visualization')).toBeInTheDocument()
    })

    const smallButton = screen.getByText('Small')
    const largeButton = screen.getByText('Large')

    await user.click(smallButton)
    expect(smallButton).toHaveClass('bg-primary-600')

    await user.click(largeButton)
    expect(largeButton).toHaveClass('bg-primary-600')
  })

  it('displays filled cells count', async () => {
    render(
      <BoardVisualizerEnhanced
        boardState={mockBoardState}
        rows={3}
        cols={3}
        depth={6}
        puzzleName="eternity2"
        configName="test_config"
      />
    )

    await waitFor(() => {
      expect(screen.getByText(/6 \/ 9 cells filled/)).toBeInTheDocument()
    })
  })

  it('opens cell details modal when clicking a cell', async () => {
    const user = userEvent.setup()

    render(
      <BoardVisualizerEnhanced
        boardState={mockBoardState}
        rows={3}
        cols={3}
        depth={6}
        puzzleName="eternity2"
        configName="test_config"
      />
    )

    await waitFor(() => {
      expect(screen.getByText('Enhanced Board Visualization')).toBeInTheDocument()
    })

    // Find a cell with a piece (using PieceVisualizer)
    const pieceVisualizers = screen.getAllByTestId('piece-visualizer')
    expect(pieceVisualizers.length).toBeGreaterThan(0)

    // Click on the first piece's parent button
    const firstPieceButton = pieceVisualizers[0].closest('button')
    if (firstPieceButton) {
      await user.click(firstPieceButton)

      await waitFor(() => {
        expect(screen.getByTestId('cell-details-modal')).toBeInTheDocument()
      })
    }
  })

  it('handles API error gracefully', async () => {
    vi.mocked(fetch).mockRejectedValue(new Error('Network error'))

    render(
      <BoardVisualizerEnhanced
        boardState={mockBoardState}
        rows={3}
        cols={3}
        depth={6}
        puzzleName="eternity2"
        configName="test_config"
      />
    )

    // Should still render something after error
    await waitFor(() => {
      expect(screen.queryByText('Loading piece definitions...')).not.toBeInTheDocument()
    })
  })

  it('renders with position to sequence mapping', async () => {
    const positionToSequence = {
      '0,0': 1,
      '0,1': 2,
      '1,0': 3,
      '1,2': 4,
      '2,1': 5,
      '2,2': 6,
    }

    render(
      <BoardVisualizerEnhanced
        boardState={mockBoardState}
        rows={3}
        cols={3}
        depth={6}
        puzzleName="eternity2"
        configName="test_config"
        positionToSequence={positionToSequence}
      />
    )

    await waitFor(() => {
      expect(screen.getByText('Enhanced Board Visualization')).toBeInTheDocument()
    })
  })

  it('displays number of fixed pieces', async () => {
    render(
      <BoardVisualizerEnhanced
        boardState={mockBoardState}
        rows={3}
        cols={3}
        depth={6}
        puzzleName="eternity2"
        configName="test_config"
        numFixedPieces={4}
      />
    )

    await waitFor(() => {
      expect(screen.getByText(/4 fixed/)).toBeInTheDocument()
    })
  })

  it('handles empty board state', async () => {
    const emptyBoard = [
      [null, null, null],
      [null, null, null],
      [null, null, null],
    ]

    render(
      <BoardVisualizerEnhanced
        boardState={emptyBoard}
        rows={3}
        cols={3}
        depth={0}
        puzzleName="eternity2"
        configName="test_config"
      />
    )

    await waitFor(() => {
      expect(screen.getByText(/0 \/ 9 cells filled/)).toBeInTheDocument()
    })
  })

  it('renders with different board dimensions', async () => {
    const largeBoard = Array.from({ length: 5 }, () =>
      Array.from({ length: 5 }, () => null)
    )

    render(
      <BoardVisualizerEnhanced
        boardState={largeBoard}
        rows={5}
        cols={5}
        depth={0}
        puzzleName="eternity2"
        configName="test_config"
      />
    )

    await waitFor(() => {
      expect(screen.getByText(/0 \/ 25 cells filled/)).toBeInTheDocument()
    })
  })
})
