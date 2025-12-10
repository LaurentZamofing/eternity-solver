import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import ConnectionStatus from './ConnectionStatus'

describe('ConnectionStatus', () => {
  it('renders connected status with green indicator', () => {
    render(<ConnectionStatus connected={true} />)

    expect(screen.getByText('Connected')).toBeInTheDocument()
    const indicator = screen.getByText('Connected').previousSibling
    expect(indicator).toHaveClass('bg-green-500')
  })

  it('renders disconnected status with red indicator', () => {
    render(<ConnectionStatus connected={false} />)

    expect(screen.getByText('Disconnected')).toBeInTheDocument()
    const indicator = screen.getByText('Disconnected').previousSibling
    expect(indicator).toHaveClass('bg-red-500')
  })

  it('applies pulse animation when connected', () => {
    const { container } = render(<ConnectionStatus connected={true} />)

    const indicator = container.querySelector('.animate-pulse')
    expect(indicator).toBeInTheDocument()
  })

  it('does not apply pulse animation when disconnected', () => {
    const { container } = render(<ConnectionStatus connected={false} />)

    const indicator = container.querySelector('.animate-pulse')
    expect(indicator).not.toBeInTheDocument()
  })
})
