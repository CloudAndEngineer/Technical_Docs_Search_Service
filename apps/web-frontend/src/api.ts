export type Notebook = {
  notebookId: string
  title: string
  createdAt: string
}

export type Session = {
  sessionId: string
  notebookId: string
  createdAt: string
}

export type ChatSource = {
  reference: number
  documentTitle: string | null
  fileName: string | null
  sourceUrl: string | null
}

export type ChatResponse = {
  answer: string
  sources: ChatSource[]
}

type ApiErrorBody = {
  message?: string
  code?: string
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const headers = new Headers(init?.headers)
  if (init?.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }
  const response = await fetch(path, {
    ...init,
    headers,
  })

  if (!response.ok) {
    const body = await response.json().catch(() => null) as ApiErrorBody | null
    throw new Error(body?.message || `요청에 실패했습니다. (HTTP ${response.status})`)
  }

  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

export const api = {
  health: () => request<{ status: string }>('/actuator/health'),
  listNotebooks: () => request<Notebook[]>('/api/v1/notebooks'),
  createNotebook: (title: string) => request<Notebook>('/api/v1/notebooks', {
    method: 'POST',
    body: JSON.stringify({ title }),
  }),
  deleteNotebook: (notebookId: string) => request<void>(`/api/v1/notebooks/${notebookId}`, {
    method: 'DELETE',
  }),
  listSessions: (notebookId: string) => request<Session[]>(`/api/v1/notebooks/${notebookId}/sessions`),
  createSession: (notebookId: string) => request<Session>(`/api/v1/notebooks/${notebookId}/sessions`, {
    method: 'POST',
  }),
  chat: (sessionId: string, question: string) => request<ChatResponse>(`/api/v1/sessions/${sessionId}/chat`, {
    method: 'POST',
    body: JSON.stringify({ question }),
  }),
}
