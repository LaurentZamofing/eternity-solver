import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import PieceVisualizer from './PieceVisualizer'

describe('PieceVisualizer', () => {
  it('renders piece with label', () => {
    render(
      <PieceVisualizer
        pieceId={42}
        north={1}
        east={2}
        south={3}
        west={4}
        rotation={0}
        showLabel={true}
      />
    )

    expect(screen.getByText('42')).toBeInTheDocument()
  })

  it('renders without label when showLabel is false', () => {
    render(
      <PieceVisualizer
        pieceId={42}
        north={1}
        east={2}
        south={3}
        west={4}
        rotation={0}
        showLabel={false}
      />
    )

    expect(screen.queryByText('42')).not.toBeInTheDocument()
  })

  it('applies rotation transform', () => {
    const { container } = render(
      <PieceVisualizer
        pieceId={1}
        north={1}
        east={2}
        south={3}
        west={4}
        rotation={90}
        showLabel={true}
      />
    )

    const rotatedElement = container.querySelector('[style*="rotate(90deg)"]')
    expect(rotatedElement).toBeInTheDocument()
  })

  it('renders with all edge patterns', () => {
    const { container } = render(
      <PieceVisualizer
        pieceId={1}
        north={5}
        east={10}
        south={15}
        west={20}
        rotation={0}
        showLabel={true}
      />
    )

    // Check that pattern images are loaded
    const patterns = container.querySelectorAll('[style*="background-image"]')
    expect(patterns.length).toBeGreaterThan(0)
  })
})
