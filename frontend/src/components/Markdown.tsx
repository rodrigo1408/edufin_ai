import type { ReactNode } from 'react'

/**
 * Renderizador seguro de um subconjunto de Markdown (títulos ##/###, listas, listas numeradas,
 * citações e **negrito**). Gera elementos React — nunca HTML bruto —, então não há risco de XSS.
 */
export function Markdown({ text }: { text: string }) {
  const blocks = text.replace(/\r\n/g, '\n').split(/\n{2,}/)
  return (
    <div className="markdown">
      {blocks.map((block, i) => renderBlock(block.trim(), i))}
    </div>
  )
}

function renderBlock(block: string, key: number): ReactNode {
  if (!block) return null
  const lines = block.split('\n')
  if (block.startsWith('### ')) return <h3 key={key}>{inline(block.slice(4))}</h3>
  if (block.startsWith('## ')) return <h2 key={key}>{inline(block.slice(3))}</h2>
  if (lines.every((l) => l.startsWith('- '))) {
    return <ul key={key}>{lines.map((l, i) => <li key={i}>{inline(l.slice(2))}</li>)}</ul>
  }
  if (lines.every((l) => /^\d+\.\s/.test(l))) {
    return <ol key={key}>{lines.map((l, i) => <li key={i}>{inline(l.replace(/^\d+\.\s/, ''))}</li>)}</ol>
  }
  if (lines.every((l) => l.startsWith('>'))) {
    return <blockquote key={key}>{inline(lines.map((l) => l.replace(/^>\s?/, '')).join(' '))}</blockquote>
  }
  return <p key={key}>{inline(lines.join(' '))}</p>
}

function inline(text: string): ReactNode[] {
  return text.split(/(\*\*[^*]+\*\*)/g).map((part, i) =>
    part.startsWith('**') && part.endsWith('**') ? <strong key={i}>{part.slice(2, -2)}</strong> : part,
  )
}
