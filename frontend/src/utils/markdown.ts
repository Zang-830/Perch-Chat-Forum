function escapeHtml(value: string) {
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;')
}

function safeUrl(value: string) {
  const url = value.trim()
  if (/^(https?:|mailto:)/i.test(url) || /^(\/|#|\.\.?\/)/.test(url)) {
    return escapeHtml(url)
  }
  return '#'
}

function renderInline(source: string) {
  const tokens: string[] = []
  const save = (html: string) => {
    const token = `\u0000MARKDOWN_TOKEN_${tokens.length}\u0000`
    tokens.push(html)
    return token
  }

  let value = source
    .replace(/`([^`\n]+)`/g, (_match, code: string) => save(`<code>${escapeHtml(code)}</code>`))
    .replace(/!\[([^\]\n]*)]\(([^)\s]+)\)/g, (_match, alt: string, url: string) =>
      save(`<img src="${safeUrl(url)}" alt="${escapeHtml(alt)}" loading="lazy">`),
    )
    .replace(/\[([^\]\n]+)]\(([^)\s]+)\)/g, (_match, label: string, url: string) => {
      const href = safeUrl(url)
      const external = /^https?:/i.test(url) ? ' target="_blank" rel="noopener noreferrer"' : ''
      return save(`<a href="${href}"${external}>${escapeHtml(label)}</a>`)
    })

  value = escapeHtml(value)
    .replace(/\*\*([^*\n]+)\*\*/g, '<strong>$1</strong>')
    .replace(/__([^_\n]+)__/g, '<strong>$1</strong>')
    .replace(/~~([^~\n]+)~~/g, '<del>$1</del>')
    .replace(/(^|[^*])\*([^*\n]+)\*/g, '$1<em>$2</em>')
    .replace(/(^|[^_])_([^_\n]+)_/g, '$1<em>$2</em>')

  return value.replace(/\u0000MARKDOWN_TOKEN_(\d+)\u0000/g, (_match, index: string) => tokens[Number(index)] ?? '')
}

function startsBlock(line: string) {
  return /^(```|#{1,6}\s|>\s?|[-*+]\s|\d+\.\s|(?:-{3,}|\*{3,})\s*$)/.test(line)
}

export function markdownToHtml(source: string) {
  const lines = source.replace(/\r\n?/g, '\n').split('\n')
  const output: string[] = []

  for (let index = 0; index < lines.length;) {
    const line = lines[index] ?? ''
    if (!line.trim()) {
      index += 1
      continue
    }

    const fence = line.match(/^```([\w-]*)\s*$/)
    if (fence) {
      const code: string[] = []
      index += 1
      while (index < lines.length && !/^```\s*$/.test(lines[index] ?? '')) {
        code.push(lines[index] ?? '')
        index += 1
      }
      if (index < lines.length) index += 1
      const language = fence[1] ? ` class="language-${escapeHtml(fence[1])}"` : ''
      output.push(`<pre><code${language}>${escapeHtml(code.join('\n'))}</code></pre>`)
      continue
    }

    const heading = line.match(/^(#{1,6})\s+(.+)$/)
    if (heading) {
      const level = heading[1]?.length ?? 1
      output.push(`<h${level}>${renderInline(heading[2] ?? '')}</h${level}>`)
      index += 1
      continue
    }

    if (/^(?:-{3,}|\*{3,})\s*$/.test(line)) {
      output.push('<hr>')
      index += 1
      continue
    }

    if (/^>\s?/.test(line)) {
      const quote: string[] = []
      while (index < lines.length && /^>\s?/.test(lines[index] ?? '')) {
        quote.push((lines[index] ?? '').replace(/^>\s?/, ''))
        index += 1
      }
      output.push(`<blockquote>${markdownToHtml(quote.join('\n'))}</blockquote>`)
      continue
    }

    const listMatch = line.match(/^([-*+]|\d+\.)\s+(.+)$/)
    if (listMatch) {
      const ordered = /\d+\./.test(listMatch[1] ?? '')
      const tag = ordered ? 'ol' : 'ul'
      const items: string[] = []
      while (index < lines.length) {
        const item = (lines[index] ?? '').match(/^([-*+]|\d+\.)\s+(.+)$/)
        if (!item || /\d+\./.test(item[1] ?? '') !== ordered) break
        items.push(`<li>${renderInline(item[2] ?? '')}</li>`)
        index += 1
      }
      output.push(`<${tag}>${items.join('')}</${tag}>`)
      continue
    }

    const paragraph: string[] = [line]
    index += 1
    while (index < lines.length && (lines[index] ?? '').trim() && !startsBlock(lines[index] ?? '')) {
      paragraph.push(lines[index] ?? '')
      index += 1
    }
    output.push(`<p>${paragraph.map(renderInline).join('<br>')}</p>`)
  }

  return output.join('')
}
