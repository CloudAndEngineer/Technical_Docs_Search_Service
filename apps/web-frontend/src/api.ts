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

export type IngestionJob = {
  jobId: string
  notebookId: string
  type: string
  status: 'QUEUED' | 'PROCESSING' | 'COMPLETED' | 'FAILED'
  documentTitle: string | null
  fileName: string | null
  errorCode: string | null
  errorMessage: string | null
  createdAt: string
  updatedAt: string
}

type ApiErrorBody = {
  message?: string
  code?: string
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const headers = new Headers(init?.headers)
  if (init?.body && !(init.body instanceof FormData) && !headers.has('Content-Type')) {
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
  uploadDocument: (notebookId: string, file: File, title: string) => {
    const body = new FormData()
    body.append('file', file)
    if (title.trim()) body.append('title', title.trim())
    return request<IngestionJob>(`/api/v1/notebooks/${notebookId}/documents`, {
      method: 'POST',
      body,
    })
  },
  getIngestionJob: (jobId: string) => request<IngestionJob>(`/api/v1/ingestion-jobs/${jobId}`),
}
