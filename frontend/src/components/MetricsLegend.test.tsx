import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MetricsLegend } from './MetricsLegend'

describe('MetricsLegend', () => {
  it('renders the legend header', () => {
    render(<MetricsLegend />)

    expect(screen.getByText('Understanding Progress Metrics')).toBeInTheDocument()
    expect(
      screen.getByText(/Two types of progress are tracked/)
    ).toBeInTheDocument()
  })

  it('shows "Learn More" button initially', () => {
    render(<MetricsLegend />)

    expect(screen.getByText('Learn More')).toBeInTheDocument()
  })

  it('does not show detailed content initially', () => {
    render(<MetricsLegend />)

    expect(screen.queryByText('Physical %:')).not.toBeInTheDocument()
    expect(screen.queryByText('Search Space:')).not.toBeInTheDocument()
  })

  it('toggles content when clicking "Learn More"', async () => {
    const user = userEvent.setup()
    render(<MetricsLegend />)

    const button = screen.getByText('Learn More')
    await user.click(button)

    // Content should now be visible
    expect(screen.getByText('Physical %:')).toBeInTheDocument()
    expect(screen.getByText('Search Space:')).toBeInTheDocument()
    expect(screen.getByText('Δ Delta:')).toBeInTheDocument()
    expect(screen.getByText('What Delta Tells You:')).toBeInTheDocument()

    // Button text should change
    expect(screen.getByText('Hide')).toBeInTheDocument()
    expect(screen.queryByText('Learn More')).not.toBeInTheDocument()
  })

  it('hides content when clicking "Hide"', async () => {
    const user = userEvent.setup()
    render(<MetricsLegend />)

    // First open
    await user.click(screen.getByText('Learn More'))
    expect(screen.getByText('Physical %:')).toBeInTheDocument()

    // Then close
    await user.click(screen.getByText('Hide'))
    expect(screen.queryByText('Physical %:')).not.toBeInTheDocument()
    expect(screen.getByText('Learn More')).toBeInTheDocument()
  })

  it('displays detailed metrics explanations when expanded', async () => {
    const user = userEvent.setup()
    render(<MetricsLegend />)

    await user.click(screen.getByText('Learn More'))

    expect(
      screen.getByText(/Actual pieces placed on the board/)
    ).toBeInTheDocument()
    expect(
      screen.getByText(/Solver's estimation of explored search space/)
    ).toBeInTheDocument()
    expect(
      screen.getByText(/Difference between search space and physical progress/)
    ).toBeInTheDocument()
  })

  it('displays delta interpretation guide when expanded', async () => {
    const user = userEvent.setup()
    render(<MetricsLegend />)

    await user.click(screen.getByText('Learn More'))

    expect(screen.getByText(/High positive/)).toBeInTheDocument()
    expect(screen.getByText(/Moderate/)).toBeInTheDocument()
    expect(screen.getByText(/Low/)).toBeInTheDocument()
  })
})
