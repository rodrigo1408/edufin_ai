import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { Markdown } from './Markdown'

describe('Markdown', () => {
  it('renderiza títulos, listas, citações e negrito', () => {
    render(<Markdown text={'## Título\n\nTexto com **destaque**.\n\n- item 1\n- item 2\n\n> Dica importante'} />)
    expect(screen.getByRole('heading', { level: 2, name: 'Título' })).toBeInTheDocument()
    expect(screen.getByText('destaque').tagName).toBe('STRONG')
    expect(screen.getAllByRole('listitem')).toHaveLength(2)
    expect(screen.getByText('Dica importante').closest('blockquote')).not.toBeNull()
  })

  it('não interpreta HTML (proteção contra XSS)', () => {
    const { container } = render(<Markdown text={'<img src=x onerror="alert(1)"><script>alert(1)</script>'} />)
    expect(container.querySelector('img')).toBeNull()
    expect(container.querySelector('script')).toBeNull()
    expect(container.textContent).toContain('<script>')
  })
})
