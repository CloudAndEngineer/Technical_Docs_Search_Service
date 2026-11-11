import { useEffect, useMemo, useRef, useState, type FormEvent, type KeyboardEvent } from 'react'
import {
  api,
  type ChatSource,
  type Notebook,
  type Session,
} from './api'

type Message = {
  id: string
  role: 'user' | 'assistant'
  content: string
  sources?: ChatSource[]
  failed?: boolean
}

function Icon({ name, size = 18 }: { name: string; size?: number }) {
  const common = {
    width: size,
    height: size,
    viewBox: '0 0 24 24',
    fill: 'none',
    stroke: 'currentColor',
    strokeWidth: 1.8,
    strokeLinecap: 'round' as const,
    strokeLinejoin: 'round' as const,
    'aria-hidden': true as const,
  }

  switch (name) {
    case 'plus': return <svg {...common}><path d="M12 5v14M5 12h14" /></svg>
    case 'book': return <svg {...common}><path d="M4 5.5A2.5 2.5 0 0 1 6.5 3H20v17H6.5A2.5 2.5 0 0 0 4 22z" /><path d="M4 5.5V22M8 7h8M8 11h7" /></svg>
    case 'chat': return <svg {...common}><path d="M20 11.5a7.5 7.5 0 0 1-7.5 7.5H6l-3 2v-6.5A7.5 7.5 0 1 1 20 11.5Z" /><path d="M8 11h.01M12 11h.01M16 11h.01" /></svg>
    case 'trash': return <svg {...common}><path d="M4 7h16M10 11v6M14 11v6M5.5 7l1 14h11l1-14M9 7V4h6v3" /></svg>
    case 'send': return <svg {...common}><path d="m21 3-7.2 18-3.6-7.2L3 10.2 21 3Z" /><path d="M10.2 13.8 21 3" /></svg>
    case 'spark': return <svg {...common}><path d="m12 3 1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8L12 3Z" /><path d="m19 16 .8 2.2L22 19l-2.2.8L19 22l-.8-2.2L16 19l2.2-.8L19 16Z" /></svg>
    case 'arrow': return <svg {...common}><path d="M5 12h14M13 6l6 6-6 6" /></svg>
    case 'close': return <svg {...common}><path d="m6 6 12 12M18 6 6 18" /></svg>
    case 'menu': return <svg {...common}><path d="M4 7h16M4 12h16M4 17h16" /></svg>
    case 'external': return <svg {...common}><path d="M14 4h6v6M20 4l-9 9" /><path d="M18 13v6a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V7a1 1 0 0 1 1-1h6" /></svg>
    case 'file': return <svg {...common}><path d="M13 3H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V10z" /><path d="M13 3v7h7M8 14h8M8 18h6" /></svg>
    default: return null
  }
}

function App() {
  const [notebooks, setNotebooks] = useState<Notebook[]>([])
  const [sessions, setSessions] = useState<Session[]>([])
  const [selectedNotebookId, setSelectedNotebookId] = useState<string | null>(null)
  const [selectedSessionId, setSelectedSessionId] = useState<string | null>(null)
  const [messages, setMessages] = useState<Message[]>([])
  const [draft, setDraft] = useState('')
  const [apiStatus, setApiStatus] = useState<'checking' | 'online' | 'offline'>('checking')
  const [loadingNotebooks, setLoadingNotebooks] = useState(true)
  const [loadingSessions, setLoadingSessions] = useState(false)
  const [sending, setSending] = useState(false)
  const [creatingNotebook, setCreatingNotebook] = useState(false)
  const [newNotebookTitle, setNewNotebookTitle] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [mobileSidebarOpen, setMobileSidebarOpen] = useState(false)
  const feedRef = useRef<HTMLDivElement>(null)

  const activeNotebook = useMemo(
    () => notebooks.find((notebook) => notebook.notebookId === selectedNotebookId) ?? null,
    [notebooks, selectedNotebookId],
  )
  const activeSession = useMemo(
    () => sessions.find((session) => session.sessionId === selectedSessionId) ?? null,
    [sessions, selectedSessionId],
  )

  useEffect(() => {
    void api.health()
      .then(() => setApiStatus('online'))
      .catch(() => setApiStatus('offline'))

    void loadNotebooks()
  }, [])

  useEffect(() => {
    if (!selectedNotebookId) {
      setSessions([])
      setSelectedSessionId(null)
      return
    }

    let active = true
    setLoadingSessions(true)
    setSelectedSessionId(null)
    setMessages([])
    setError('')

    api.listSessions(selectedNotebookId)
      .then((items) => {
        if (!active) return
        setSessions(items)
        setSelectedSessionId(items[0]?.sessionId ?? null)
      })
      .catch((reason: unknown) => {
        if (active) setError(errorMessage(reason))
      })
      .finally(() => {
        if (active) setLoadingSessions(false)
      })

    return () => { active = false }
  }, [selectedNotebookId])

  useEffect(() => {
    if (feedRef.current) {
      feedRef.current.scrollTo({ top: feedRef.current.scrollHeight, behavior: 'smooth' })
    }
  }, [messages, sending])

  async function loadNotebooks() {
    setLoadingNotebooks(true)
    try {
      const items = await api.listNotebooks()
      setNotebooks(items)
      setApiStatus('online')
      setSelectedNotebookId((current) => {
        if (current && items.some((item) => item.notebookId === current)) return current
        return items[0]?.notebookId ?? null
      })
    } catch (reason) {
      setApiStatus('offline')
      setError(errorMessage(reason))
    } finally {
      setLoadingNotebooks(false)
    }
  }

  async function handleCreateNotebook(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const title = newNotebookTitle.trim()
    if (!title || busy) return
    setBusy(true)
    setError('')
    try {
      const notebook = await api.createNotebook(title)
      setNotebooks((current) => [notebook, ...current])
      setSelectedNotebookId(notebook.notebookId)
      setSelectedSessionId(null)
      setMessages([])
      setNewNotebookTitle('')
      setCreatingNotebook(false)
      setMobileSidebarOpen(false)
    } catch (reason) {
      setError(errorMessage(reason))
    } finally {
      setBusy(false)
    }
  }

  async function handleDeleteNotebook(notebook: Notebook) {
    if (!window.confirm(`“${notebook.title}” 노트북과 안의 세션을 삭제할까요?`)) return
    setError('')
    try {
      await api.deleteNotebook(notebook.notebookId)
      const remaining = notebooks.filter((item) => item.notebookId !== notebook.notebookId)
      setNotebooks(remaining)
      if (selectedNotebookId === notebook.notebookId) {
        setSelectedNotebookId(remaining[0]?.notebookId ?? null)
        setSelectedSessionId(null)
        setMessages([])
      }
    } catch (reason) {
      setError(errorMessage(reason))
    }
  }

  async function handleCreateSession() {
    if (!selectedNotebookId || busy) return
    setBusy(true)
    setError('')
    try {
      const session = await api.createSession(selectedNotebookId)
      setSessions((current) => [session, ...current])
      setSelectedSessionId(session.sessionId)
      setMessages([])
      setDraft('')
      setMobileSidebarOpen(false)
    } catch (reason) {
      setError(errorMessage(reason))
    } finally {
      setBusy(false)
    }
  }

  async function handleSend(event?: FormEvent<HTMLFormElement>, preset?: string) {
    event?.preventDefault()
    const question = (preset ?? draft).trim()
    if (!question || !selectedSessionId || sending) return

    const userMessage: Message = { id: crypto.randomUUID(), role: 'user', content: question }
    setMessages((current) => [...current, userMessage])
    setDraft('')
    setSending(true)
    setError('')

    try {
      const response = await api.chat(selectedSessionId, question)
      setMessages((current) => [...current, {
        id: crypto.randomUUID(),
        role: 'assistant',
        content: response.answer,
        sources: response.sources,
      }])
    } catch (reason) {
      setMessages((current) => [...current, {
        id: crypto.randomUUID(),
        role: 'assistant',
        content: errorMessage(reason),
        failed: true,
      }])
    } finally {
      setSending(false)
    }
  }

  function handleComposerKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault()
      void handleSend()
    }
  }

  function selectNotebook(notebookId: string) {
    setSelectedNotebookId(notebookId)
    setMobileSidebarOpen(false)
  }

  return (
    <div className="app-shell">
      {mobileSidebarOpen && <button className="mobile-scrim" aria-label="메뉴 닫기" onClick={() => setMobileSidebarOpen(false)} />}
      <aside className={`sidebar ${mobileSidebarOpen ? 'sidebar-open' : ''}`}>
        <div className="brand-row">
          <div className="brand-mark"><Icon name="spark" size={21} /></div>
          <div className="brand-copy"><strong>노트북</strong><span>기술 지식 워크스페이스</span></div>
          <button className="icon-button sidebar-close" aria-label="메뉴 닫기" onClick={() => setMobileSidebarOpen(false)}><Icon name="close" /></button>
        </div>

        <div className="sidebar-section-heading">
          <span>내 워크스페이스</span>
          <button className="small-icon-button" aria-label="노트북 만들기" title="노트북 만들기" onClick={() => setCreatingNotebook((value) => !value)}><Icon name="plus" size={17} /></button>
        </div>

        {creatingNotebook && (
          <form className="create-notebook-form" onSubmit={handleCreateNotebook}>
            <input autoFocus maxLength={120} value={newNotebookTitle} onChange={(event) => setNewNotebookTitle(event.target.value)} placeholder="노트북 이름" aria-label="새 노트북 이름" />
            <div className="form-actions">
              <button type="button" className="text-button" onClick={() => { setCreatingNotebook(false); setNewNotebookTitle('') }}>취소</button>
              <button type="submit" className="mini-primary" disabled={!newNotebookTitle.trim() || busy}>{busy ? '생성 중…' : '만들기'}</button>
            </div>
          </form>
        )}

        <div className="notebook-list" aria-label="노트북 목록">
          {loadingNotebooks ? <div className="sidebar-skeleton"><i /><i /><i /></div> : notebooks.length === 0 ? (
            <div className="sidebar-empty"><span className="empty-glyph"><Icon name="book" /></span><p>아직 노트북이 없습니다.</p><button className="text-button accent-text" onClick={() => setCreatingNotebook(true)}>첫 노트북 만들기</button></div>
          ) : notebooks.map((notebook) => (
            <div className={`notebook-row ${selectedNotebookId === notebook.notebookId ? 'selected' : ''}`} key={notebook.notebookId}>
              <button className="notebook-select" onClick={() => selectNotebook(notebook.notebookId)}>
                <span className="notebook-icon"><Icon name="book" size={17} /></span>
                <span className="notebook-title-wrap"><span className="notebook-title">{notebook.title}</span><span className="notebook-subtitle">공용 노트북</span></span>
              </button>
              <button className="row-action" aria-label={`${notebook.title} 삭제`} title="노트북 삭제" onClick={() => void handleDeleteNotebook(notebook)}><Icon name="trash" size={15} /></button>
            </div>
          ))}
        </div>

        <div className="session-section">
          <div className="sidebar-section-heading session-heading"><span>대화</span><span className="count-pill">{sessions.length}</span></div>
          {selectedNotebookId && <button className="new-session-button" onClick={() => void handleCreateSession()} disabled={busy}><Icon name="plus" size={16} /><span>새 대화</span></button>}
          {!selectedNotebookId ? <p className="muted-note">노트북을 선택하면 대화가 표시됩니다.</p> : loadingSessions ? <div className="session-loading">대화를 불러오는 중…</div> : sessions.length === 0 ? <p className="muted-note">이 노트북에 대화가 없습니다.</p> : (
            <div className="session-list">
              {sessions.map((session) => (
                <button key={session.sessionId} className={`session-row ${selectedSessionId === session.sessionId ? 'active' : ''}`} onClick={() => { setSelectedSessionId(session.sessionId); setMessages([]); setMobileSidebarOpen(false) }}>
                  <Icon name="chat" size={16} /><span>새 대화</span><span className="session-date">{formatShortDate(session.createdAt)}</span>
                </button>
              ))}
            </div>
          )}
        </div>

        <div className="sidebar-footer">
          <div className={`connection-dot ${apiStatus}`} />
          <span>{apiStatus === 'online' ? 'API 연결됨' : apiStatus === 'checking' ? '연결 확인 중' : 'API 연결 안 됨'}</span>
          <span className="footer-version">v1</span>
        </div>
      </aside>

      <main className="main-panel">
        <header className="topbar">
          <button className="icon-button mobile-menu" aria-label="메뉴 열기" onClick={() => setMobileSidebarOpen(true)}><Icon name="menu" /></button>
          <div className="breadcrumbs"><span>워크스페이스</span><span className="crumb-divider">/</span><strong>{activeNotebook?.title ?? '노트북 선택'}</strong></div>
          <div className="topbar-right">
            <div className={`api-badge ${apiStatus}`}><span />{apiStatus === 'online' ? '시스템 정상' : apiStatus === 'checking' ? '확인 중' : '연결 확인 필요'}</div>
            <button className="topbar-new-chat" disabled={!activeNotebook || busy} onClick={() => void handleCreateSession()}><Icon name="plus" size={16} /><span>새 대화</span></button>
          </div>
        </header>

        <section className="workspace-content">
          {error && <div className="error-banner" role="alert"><span>{error}</span><button aria-label="오류 닫기" onClick={() => setError('')}><Icon name="close" size={15} /></button></div>}
          {!activeNotebook ? (
            <div className="welcome-state">
              <div className="welcome-orbit"><div className="welcome-icon"><Icon name="spark" size={30} /></div><span className="orbit orbit-one" /><span className="orbit orbit-two" /></div>
              <p className="eyebrow">RAG KNOWLEDGE WORKSPACE</p>
              <h1>기술 지식을 모으고<br /><em>대화로 탐색하세요.</em></h1>
              <p className="welcome-description">노트북을 만들고, 문서를 바탕으로 필요한 답을 찾아보세요.</p>
              <button className="primary-button" onClick={() => { setCreatingNotebook(true); setMobileSidebarOpen(true) }}><Icon name="plus" size={17} />첫 노트북 만들기</button>
              <div className="welcome-caption"><span className="caption-line" />모든 노트북은 이 서버의 공용 작업 공간입니다.<span className="caption-line" /></div>
            </div>
          ) : !activeSession ? (
            <div className="welcome-state session-welcome">
              <div className="welcome-orbit small-orbit"><div className="welcome-icon"><Icon name="chat" size={27} /></div><span className="orbit orbit-one" /><span className="orbit orbit-two" /></div>
              <p className="eyebrow">{activeNotebook.title}</p>
              <h1>무엇을 알아볼까요?</h1>
              <p className="welcome-description">새 대화를 시작해 이 노트북의 지식에 질문해 보세요.</p>
              <button className="primary-button" onClick={() => void handleCreateSession()} disabled={busy}><Icon name="plus" size={17} />대화 시작하기</button>
            </div>
          ) : (
            <div className="chat-layout">
              <div className="chat-heading">
                <div className="chat-heading-icon"><Icon name="spark" size={18} /></div>
                <div><h1>{activeNotebook.title}</h1><p>노트북의 기술 자료를 바탕으로 답변합니다</p></div>
                <span className="session-chip"><span className="session-chip-dot" />대화 세션</span>
              </div>

              <div className="message-feed" ref={feedRef}>
                {messages.length === 0 ? (
                  <div className="conversation-intro">
                    <div className="assistant-avatar"><Icon name="spark" size={17} /></div>
                    <div className="intro-card"><strong>안녕하세요! 무엇을 도와드릴까요?</strong><p>이 노트북에 등록된 기술 자료를 바탕으로 답변해 드릴게요.</p><div className="suggestion-list"><button onClick={() => void handleSend(undefined, '이 노트북에서 어떤 내용을 찾을 수 있나요?')}>이 노트북에서 어떤 내용을 찾을 수 있나요?<Icon name="arrow" size={14} /></button><button onClick={() => void handleSend(undefined, '등록된 자료의 핵심 내용을 요약해줘')}>등록된 자료의 핵심 내용을 요약해줘<Icon name="arrow" size={14} /></button></div></div>
                  </div>
                ) : messages.map((message) => (
                  <article className={`message-row ${message.role}`} key={message.id}>
                    {message.role === 'assistant' && <div className={`assistant-avatar ${message.failed ? 'avatar-error' : ''}`}><Icon name={message.failed ? 'close' : 'spark'} size={16} /></div>}
                    <div className={`message-content ${message.failed ? 'message-failed' : ''}`}>
                      {message.role === 'assistant' && <div className="message-label">{message.failed ? '요청 오류' : 'AI 답변'}</div>}
                      <div className="message-text">{message.content}</div>
                      {!!message.sources?.length && <div className="source-block"><div className="source-heading"><span>참고 자료</span><span className="source-count">{message.sources.length}</span></div><div className="source-list">{message.sources.map((source) => <SourceCard key={`${message.id}-${source.reference}`} source={source} />)}</div></div>}
                    </div>
                    {message.role === 'user' && <div className="user-avatar">나</div>}
                  </article>
                ))}
                {sending && <div className="message-row assistant"><div className="assistant-avatar"><Icon name="spark" size={16} /></div><div className="typing-card"><span /><span /><span /><small>자료를 살펴보고 있어요</small></div></div>}
              </div>

              <div className="composer-area">
                <form className="composer" onSubmit={(event) => void handleSend(event)}>
                  <textarea value={draft} onChange={(event) => setDraft(event.target.value)} onKeyDown={handleComposerKeyDown} placeholder="이 노트북에 대해 질문하세요…" rows={1} aria-label="질문 입력" disabled={sending} />
                  <div className="composer-bottom"><span className="composer-hint">Enter 전송 <span>·</span> Shift + Enter 줄바꿈</span><button className="send-button" type="submit" disabled={!draft.trim() || sending} aria-label="질문 보내기"><Icon name="send" size={17} /></button></div>
                </form>
                <p className="privacy-note">답변은 등록된 자료의 내용과 다를 수 있으니 출처를 확인해 주세요.</p>
              </div>
            </div>
          )}
        </section>
      </main>
    </div>
  )
}

function SourceCard({ source }: { source: ChatSource }) {
  const title = source.documentTitle || source.fileName || source.sourceUrl || '참고 자료'
  return (
    <div className="source-card">
      <div className="source-file-icon"><Icon name={source.sourceUrl ? 'external' : 'file'} size={15} /></div>
      <div className="source-details"><span className="source-reference">자료 {source.reference}</span><strong>{title}</strong><span className="source-meta">{source.sourceUrl ? '외부 문서' : source.fileName || '업로드 자료'}</span></div>
      {source.sourceUrl && <a className="source-open" href={source.sourceUrl} target="_blank" rel="noreferrer" aria-label="출처 열기"><Icon name="external" size={15} /></a>}
    </div>
  )
}

function errorMessage(reason: unknown) {
  return reason instanceof Error ? reason.message : '요청 중 오류가 발생했습니다.'
}

function formatShortDate(value: string) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return new Intl.DateTimeFormat('ko-KR', { month: 'numeric', day: 'numeric' }).format(date)
}

export default App
