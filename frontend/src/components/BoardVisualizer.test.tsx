import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { BoardVisualizer } from './BoardVisualizer'

describe('BoardVisualizer', () => {
  const mockBoardState = [
    ['1_0', '2_1', null],
    ['3_2', null, '4_3'],
    [null, '5_0', '6_1'],
  ]

  it('renders board visualization with filled cells count', () => {
    render(
      <BoardVisualizer
        boardState={mockBoardState}
        rows={3}
        cols={3}
        depth={6}
      />
    )

    expect(screen.getByText('Board Visualization')).toBeInTheDocument()
    expect(screen.getByText('6 / 9 cells filled (6 pieces placed)')).toBeInTheDocument()
  })

  it('shows empty state when board state is not available', () => {
    render(
      <BoardVisualizer
        boardState={undefined}
        rows={3}
        cols={3}
        depth={0}
      />
    )

    expect(screen.getByText('Board visualization not available')).toBeInTheDocument()
    expect(
      screen.getByText('The save file may not contain board state data')
    ).toBeInTheDocument()
  })

  it('shows empty state when board state is empty array', () => {
    render(
      <BoardVisualizer
        boardState={[]}
        rows={3}
        cols={3}
        depth={0}
      />
    )

    expect(screen.getByText('Board visualization not available')).toBeInTheDocument()
  })

  it('renders zoom control buttons', () => {
    render(
      <BoardVisualizer
        boardState={mockBoardState}
        rows={3}
        cols={3}
        depth={6}
      />
    )

    expect(screen.getByText('Small')).toBeInTheDocument()
    expect(screen.getByText('Medium')).toBeInTheDocument()
    expect(screen.getByText('Large')).toBeInTheDocument()
  })

  it('changes zoom level when clicking zoom buttons', async () => {
    const user = userEvent.setup()
    render(
      <BoardVisualizer
        boardState={mockBoardState}
        rows={3}
        cols={3}
        depth={6}
      />
    )

    const smallButton = screen.getByText('Small')
    const largeButton = screen.getByText('Large')

    // Initially medium should be selected
    const mediumButton = screen.getByText('Medium')
    expect(mediumButton).toHaveClass('bg-primary-600')

    // Click small
    await user.click(smallButton)
    expect(smallButton).toHaveClass('bg-primary-600')

    // Click large
    await user.click(largeButton)
    expect(largeButton).toHaveClass('bg-primary-600')
  })

  it('renders board grid with correct dimensions', () => {
    const { container } = render(
      <BoardVisualizer
        boardState={mockBoardState}
        rows={3}
        cols={3}
        depth={6}
      />
    )

    // The board grid should be present
    const grid = container.querySelector('.grid')
    expect(grid).toBeInTheDocument()
  })

  it('counts filled cells correctly', () => {
    // 6 filled cells out of 9 total
    render(
      <BoardVisualizer
        boardState={mockBoardState}
        rows={3}
        cols={3}
        depth={6}
      />
    )

    expect(screen.getByText('6 / 9 cells filled (6 pieces placed)')).toBeInTheDocument()
  })

  it('handles empty board state', () => {
    const emptyBoard = [
      [null, null, null],
      [null, null, null],
      [null, null, null],
    ]

    render(
      <BoardVisualizer
        boardState={emptyBoard}
        rows={3}
        cols={3}
        depth={0}
      />
    )

    expect(screen.getByText('0 / 9 cells filled (0 pieces placed)')).toBeInTheDocument()
  })

  it('handles fully filled board state', () => {
    const fullBoard = [
      ['1_0', '2_1', '3_2'],
      ['4_3', '5_0', '6_1'],
      ['7_2', '8_3', '9_0'],
    ]

    render(
      <BoardVisualizer
        boardState={fullBoard}
        rows={3}
        cols={3}
        depth={9}
      />
    )

    expect(screen.getByText('9 / 9 cells filled (9 pieces placed)')).toBeInTheDocument()
  })

  it('renders with different board dimensions', () => {
    const largeBoard = Array.from({ length: 5 }, () =>
      Array.from({ length: 5 }, () => null)
    )

    render(
      <BoardVisualizer
        boardState={largeBoard}
        rows={5}
        cols={5}
        depth={0}
      />
    )

    expect(screen.getByText('0 / 25 cells filled (0 pieces placed)')).toBeInTheDocument()
  })
})
