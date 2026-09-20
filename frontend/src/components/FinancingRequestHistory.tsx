import { useId, useState } from 'react'
import type { FormEvent } from 'react'
import { FinancingRequestApiError, FinancingRequestNetworkError, searchFinancingRequests } from '../api/financingRequests'
import type { FinancingRequestHistoryPage } from '../api/types'
import { normalizePersonalIdentityCode, validatePersonalIdentityCode } from '../identityCode'

function formatDate(value: string) {
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}

function ErrorPanel({ error }: { error: { title: string; detail: string; reference?: string } }) {
  return <div className="error-panel" role="alert"><strong>{error.title}</strong><p>{error.detail}</p>{error.reference && <span>Reference: {error.reference}</span>}</div>
}

export function FinancingRequestHistory() {
  const inputId = useId()
  const errorId = `${inputId}-error`
  const [value, setValue] = useState('')
  const [submittedCode, setSubmittedCode] = useState<string | null>(null)
  const [page, setPage] = useState<FinancingRequestHistoryPage | null>(null)
  const [loading, setLoading] = useState(false)
  const [validationError, setValidationError] = useState('')
  const [error, setError] = useState<{ title: string; detail: string; reference?: string } | null>(null)

  async function loadHistory(code: string, pageNumber: number) {
    setLoading(true)
    setError(null)
    try {
      const result = await searchFinancingRequests({ personalIdentityCode: code, page: pageNumber, size: 20 })
      setPage(result)
      setSubmittedCode(code)
      setValue('')
    } catch (requestError: unknown) {
      if (requestError instanceof FinancingRequestApiError) {
        setError({ title: requestError.problem.title || 'History could not be loaded', detail: requestError.problem.detail || 'Please check the request and try again.', reference: requestError.problem.correlationId })
      } else if (requestError instanceof FinancingRequestNetworkError) {
        setError({ title: 'Connection problem', detail: 'Credit Lens could not be reached. Check the connection and try again.' })
      } else {
        setError({ title: 'History could not be loaded', detail: 'Something unexpected happened. Please try again.' })
      }
    } finally { setLoading(false) }
  }

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const code = normalizePersonalIdentityCode(value)
    const validation = validatePersonalIdentityCode(code)
    if (validation) { setValidationError(validation); return }
    setValidationError('')
    void loadHistory(code, 0)
  }

  const totalPages = page?.totalPages ?? 0
  const currentPage = page && totalPages > 0 ? page.page + 1 : 0
  const canGoPrevious = Boolean(page && page.page > 0)
  const canGoNext = Boolean(page && page.page + 1 < totalPages)

  return <section className="history-panel" aria-labelledby="history-title">
    <div className="section-heading"><div><p className="eyebrow">Request history</p><h2 id="history-title">Find completed requests</h2></div><span className="step-label">01 / 01</span></div>
    <p className="section-copy">Search successfully completed financing requests for one consumer.</p>
    <form className="request-form history-search-form" onSubmit={submit} noValidate>
      <div className="field-group"><label htmlFor={inputId}>Finnish personal identity code</label><input id={inputId} name="personalIdentityCode" type="text" autoComplete="off" inputMode="text" placeholder="010190-123A" value={value} onChange={(event) => { setValue(event.target.value); if (validationError) setValidationError('') }} aria-invalid={Boolean(validationError)} aria-describedby={validationError ? errorId : undefined} disabled={loading} />{validationError && <p className="field-error" id={errorId} role="alert">{validationError}</p>}</div>
      <button className="primary-button" type="submit" disabled={loading}>{loading ? 'Searching…' : 'Search request history'}</button>
    </form>
    {loading && <div className="loading-message" role="status" aria-live="polite"><span className="spinner" aria-hidden="true" /><span>Loading request history…</span></div>}
    {error && <ErrorPanel error={error} />}
    {page && !loading && page.items.length === 0 && <div className="empty-state"><h3>No completed financing requests were found for this consumer.</h3><p>Try another identity code to search again.</p></div>}
    {page && page.items.length > 0 && <>
      <div className="history-list" aria-label="Completed financing requests">{page.items.map((item) => <article className="history-card" key={item.id}><dl className="result-grid"><div><dt>Identity code</dt><dd>{item.maskedPersonalIdentityCode}</dd></div><div><dt>Requested</dt><dd>{formatDate(item.requestedAt)}</dd></div><div><dt>Completed</dt><dd>{formatDate(item.completedAt)}</dd></div><div><dt>Extract reference</dt><dd className="reference-value">{item.extractReference}</dd></div><div><dt>Voluntary credit ban</dt><dd><span className={item.voluntaryCreditBanActive ? 'ban-active' : ''}>{item.voluntaryCreditBanActive ? 'Active' : 'Not active'}</span></dd></div></dl></article>)}</div>
      <nav className="pagination" aria-label="Request history pages"><button className="secondary-button" type="button" disabled={loading || !canGoPrevious} onClick={() => submittedCode && void loadHistory(submittedCode, page.page - 1)}>Previous</button><span aria-live="polite">Page {currentPage} of {totalPages}</span><button className="secondary-button" type="button" disabled={loading || !canGoNext} onClick={() => submittedCode && void loadHistory(submittedCode, page.page + 1)}>Next</button></nav>
    </>}
  </section>
}
