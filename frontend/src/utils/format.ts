export function formatTime(value: string): string {
  const date = new Date(value)
  const diff = Date.now() - date.getTime()
  const minute = 60_000
  const hour = minute * 60
  const day = hour * 24
  if (diff < minute) return '刚刚'
  if (diff < hour) return `${Math.floor(diff / minute)} 分钟前`
  if (diff < day) return `${Math.floor(diff / hour)} 小时前`
  if (diff < day * 7) return `${Math.floor(diff / day)} 天前`
  return new Intl.DateTimeFormat('zh-CN', { month: 'short', day: 'numeric' }).format(date)
}

export function initials(name: string): string {
  return name.trim().slice(0, 2).toUpperCase()
}

