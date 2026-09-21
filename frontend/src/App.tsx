import { useRef, useState } from 'react'
import {
  createFinancingRequest,
  FinancingRequestApiError,
  FinancingRequestNetworkError,
} from './api/financingRequests'
import type { CreditRegisterExtractPurpose, FinancingRequestResponse } from './api/types'
import { FinancingRequestForm } from './components/FinancingRequestForm'
import { FinancingRequestResult } from './components/FinancingRequestResult'
import { FinancingRequestHistory } from './components/FinancingRequestHistory'
import { FinancingRequestDetailsView } from './components/FinancingRequestDetails'
import './App.css'

type RequestState = 'idle' | 'submitting' | 'completed' | 'api-error' | 'network-error'

function App() {
  const [section, setSection] = useState<'new' | 'history'>('new')
  const [detailsId, setDetailsId] = useState<string | null>(null)
  const pageTitleRef = useRef<HTMLHeadingElement>(null)
  const [state, setState] = useState<RequestState>('idle')
  const [result, setResult] = useState<FinancingRequestResponse | null>(null)
  const [error, setError] = useState<{ title: string; detail: string; reference?: string } | null>(null)
  const [clientRequestId, setClientRequestId] = useState<string | null>(null)
  const [lastSubmission, setLastSubmission] = useState<{ personalIdentityCode: string; purpose: CreditRegisterExtractPurpose } | null>(null)

  async function submitRequest(personalIdentityCode: string, purpose: CreditRegisterExtractPurpose) {
    const canRetrySameRequest = clientRequestId
      && lastSubmission?.personalIdentityCode === personalIdentityCode
      && lastSubmission.purpose === purpose
    const requestId = canRetrySameRequest ? clientRequestId : crypto.randomUUID()
    setClientRequestId(requestId)
    setLastSubmission({ personalIdentityCode, purpose })
    setState('submitting')
    setError(null)

    try {
      const response = await createFinancingRequest({
        clientRequestId: requestId,
        personalIdentityCode,
        creditRegisterExtractPurposes: [purpose],
      })
      setResult(response)
      setState('completed')
      setClientRequestId(null)
      setLastSubmission(null)
    } catch (requestError: unknown) {
      if (requestError instanceof FinancingRequestApiError) {
        setState('api-error')
        setError({
          title: requestError.problem.title || 'Request could not be completed',
          detail: requestError.problem.detail || 'Please check the request and try again.',
          reference: requestError.problem.correlationId,
        })
      } else if (requestError instanceof FinancingRequestNetworkError) {
        setState('network-error')
        setError({
          title: 'Connection problem',
          detail: 'Credit Lens could not be reached. Check the connection and try again.',
        })
      } else {
        setState('api-error')
        setError({ title: 'Request could not be completed', detail: 'Something unexpected happened. Please try again.' })
      }
    }
  }

  function createAnotherRequest() {
    setResult(null)
    setError(null)
    setClientRequestId(null)
    setLastSubmission(null)
    setState('idle')
  }

  function openDetails(id: string) {
    setSection('history')
    setDetailsId(id)
  }

  function returnToHistory() {
    setDetailsId(null)
    document.title = 'Credit Lens'
    requestAnimationFrame(() => pageTitleRef.current?.focus())
  }

  const isSubmitting = state === 'submitting'

  return (
    <div className="app-shell">
      <header className="site-header">
        <a className="brand" href="/" aria-label="Credit Lens home">
          <span className="brand-mark" aria-hidden="true">CL</span>
          <span>Credit Lens</span>
        </a>
        <span className="status-badge">Positive Credit Register</span>
      </header>
      <nav className="main-navigation" aria-label="Main navigation">
        <button className={section === 'new' ? 'nav-link active' : 'nav-link'} type="button" aria-current={section === 'new' ? 'page' : undefined} onClick={() => { setDetailsId(null); setSection('new') }}>New request</button>
        <button className={section === 'history' ? 'nav-link active' : 'nav-link'} type="button" aria-current={section === 'history' ? 'page' : undefined} onClick={() => { setDetailsId(null); setSection('history') }}>Request history</button>
      </nav>
      <main>
        {!detailsId && <section className="hero" aria-labelledby="page-title">
          <p className="eyebrow">Financing request</p>
          <h1 id="page-title" ref={pageTitleRef} tabIndex={-1}>A clearer view of every credit decision.</h1>
          <p className="hero-copy">Request a Finnish credit register extract securely and get the information you need for a financing decision.</p>
        </section>}
        <section className="workspace" aria-labelledby={detailsId ? 'details-title' : section === 'new' ? 'request-title' : 'history-title'}>
          {(section === 'history' || detailsId) && <div hidden={Boolean(detailsId)}><FinancingRequestHistory onViewDetails={openDetails} /></div>}
          {detailsId ? <FinancingRequestDetailsView id={detailsId} onBack={returnToHistory} /> : section === 'new' && <>
          {state !== 'completed' && (
            <div className="request-panel">
              <div className="section-heading">
                <div>
                  <p className="eyebrow">New request</p>
                  <h2 id="request-title">Create a financing request</h2>
                </div>
                <span className="step-label">01 / 01</span>
              </div>
              <p className="section-copy">Enter the consumer&apos;s identity code and select why the extract is needed.</p>
              {error && (
                <div className="error-panel" role="alert">
                  <strong>{error.title}</strong>
                  <p>{error.detail}</p>
                  {error.reference && <span>Reference: {error.reference}</span>}
                </div>
              )}
              {isSubmitting && (
                <div className="loading-message" role="status" aria-live="polite">
                  <span className="spinner" aria-hidden="true" />
                  <span>Requesting the credit extract…</span>
                </div>
              )}
              <FinancingRequestForm disabled={isSubmitting} onSubmit={submitRequest} />
            </div>
          )}
          {result && state === 'completed' && <FinancingRequestResult result={result} onCreateAnother={createAnotherRequest} />}
          </>}
        </section>
      </main>
      <footer>
        <span>Credit Lens</span>
        <span>Your data stays private</span>
      </footer>
    </div>
  )
}

export default App
