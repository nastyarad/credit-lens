import { useEffect, useRef, useState } from 'react'
import type { ReactNode } from 'react'
import { FinancingRequestApiError, FinancingRequestNetworkError, getFinancingRequestDetails } from '../api/financingRequests'
import type { CurrencyAmount, FinancingRequestDetails, Loan } from '../api/types'

const INCOME_CURRENCY = 'EUR'

function label(value: string) {
  const words = value.replace(/([a-z0-9])([A-Z])/g, '$1 $2').toLowerCase().replace('lump sum', 'lump-sum')
  return words.replace(/^./, (first) => first.toUpperCase())
}

function date(value: string | null) {
  return value ? new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' }).format(new Date(`${value}T00:00:00`)) : 'Not provided'
}

function instant(value: string, utc = false) {
  const formatted = new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short', ...(utc ? { timeZone: 'UTC' } : {}) }).format(new Date(value))
  return utc ? `${formatted} UTC` : formatted
}

function amount(value: number | null, currencyCode: string | null) {
  if (value === null) return 'Not provided'
  return `${currencyCode ?? 'Currency not provided'} ${new Intl.NumberFormat(undefined, { maximumFractionDigits: 2 }).format(value)}`
}

function incomeAmount(value: number | null) {
  return value === null
    ? 'Not provided'
    : `${INCOME_CURRENCY} ${new Intl.NumberFormat(undefined, { maximumFractionDigits: 2 }).format(value)}`
}

function currencyAmounts(values: CurrencyAmount[]) {
  return values.length ? values.map(({ currencyCode, sum }) => `${currencyCode} ${new Intl.NumberFormat(undefined, { maximumFractionDigits: 2 }).format(sum)}`).join(', ') : 'Not provided'
}

function Value({ label: term, children }: { label: string; children: ReactNode }) {
  return <div><dt>{term}</dt><dd>{children}</dd></div>
}

function LoanDetails({ loan, index }: { loan: Loan; index: number }) {
  const loanName = `Loan ${index + 1} — ${label(loan.loanType)}`
  return <article className="loan-card">
    <h3>{loanName}</h3>
    <dl className="result-grid detail-grid">
      <Value label="Contract date">{date(loan.contractDate)}</Value>
      <Value label="Currency">{loan.currencyCode ?? 'Not provided'}</Value>
      <Value label="Collateral">{loan.isLoanWithCollateral === null ? 'Not provided' : loan.isLoanWithCollateral ? 'Yes' : 'No'}</Value>
      <Value label="Collateral types">{loan.collateralType.length ? loan.collateralType.map(label).join(', ') : 'Not provided'}</Value>
      <Value label="Borrowers">{loan.borrowersCount ?? 'Not provided'}</Value>
      <Value label="Accuracy denied">{loan.accuracyIsDenied ? 'Yes' : 'No'}</Value>
      {loan.paymentPlan && <><Value label="Debt arrangement">{loan.paymentPlan.isInDebtArrangement ? 'Yes' : 'No'}</Value><Value label="Business restructuring">{loan.paymentPlan.isInBusinessRestructuringProgram ? 'Yes' : 'No'}</Value></>}
    </dl>
    {loan.lumpSumLoan && <section><h4>Lump-sum loan</h4><dl className="result-grid detail-grid"><Value label="Amount issued">{amount(loan.lumpSumLoan.amountIssued, loan.currencyCode)}</Value><Value label="Amount paid">{amount(loan.lumpSumLoan.amountPaid, loan.currencyCode)}</Value><Value label="Balance">{amount(loan.lumpSumLoan.balance, loan.currencyCode)}</Value><Value label="Final due date">{date(loan.lumpSumLoan.plannedFinalDueDate)}</Value><Value label="Amortization frequency">{loan.lumpSumLoan.amortizationFrequency ?? 'Not provided'}</Value></dl></section>}
    {loan.runningAccountLoan && <section><h4>Running account loan</h4><dl className="result-grid detail-grid"><Value label="Credit limit">{amount(loan.runningAccountLoan.creditLimit, loan.currencyCode)}</Value><Value label="Balance">{amount(loan.runningAccountLoan.balance, loan.currencyCode)}</Value><Value label="Balance date">{date(loan.runningAccountLoan.balanceDate)}</Value></dl></section>}
    {loan.leasingContract && <section><h4>Leasing contract</h4><dl className="result-grid detail-grid"><Value label="Contract period start">{date(loan.leasingContract.contractPeriodStartDate)}</Value><Value label="Transaction price">{amount(loan.leasingContract.transactionPrice, loan.currencyCode)}</Value></dl></section>}
  </article>
}

export function FinancingRequestDetailsView({ id, onBack }: { id: string; onBack: () => void }) {
  const headingRef = useRef<HTMLHeadingElement>(null)
  const errorRef = useRef<HTMLDivElement>(null)
  const [details, setDetails] = useState<FinancingRequestDetails | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<{ title: string; detail: string; reference?: string; notFound?: boolean } | null>(null)
  const [retry, setRetry] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    void (async () => {
      try { setDetails(await getFinancingRequestDetails(id, controller.signal)) }
      catch (requestError) {
        if (requestError instanceof DOMException && requestError.name === 'AbortError') return
        if (requestError instanceof FinancingRequestApiError) setError(requestError.status === 404
          ? { title: 'Request not found', detail: 'Return to request history and choose another request.', reference: requestError.problem.correlationId, notFound: true }
          : { title: 'We could not load request details', detail: 'Try again. If the problem continues, contact support.', reference: requestError.problem.correlationId })
        else if (requestError instanceof FinancingRequestNetworkError) setError({ title: 'We could not load request details', detail: 'Check your connection and try again.' })
        else setError({ title: 'We could not load request details', detail: 'Try again. If the problem continues, contact support.' })
      } finally { if (!controller.signal.aborted) setLoading(false) }
    })()
    return () => controller.abort()
  }, [id, retry])
  useEffect(() => { document.title = 'Credit register extract | Credit Lens'; headingRef.current?.focus() }, [])
  useEffect(() => { if (error) errorRef.current?.focus() }, [error])

  const extract = details?.creditExtract
  const summary = extract?.creditInformationSummary
  const delayed = details?.creditExtract.loans.flatMap((loan, loanIndex) => loan.delayedAmount.map((entry) => ({ ...entry, loanIndex, loanType: loan.loanType, currencyCode: loan.currencyCode }))) ?? []
  return <section className="details-panel" aria-labelledby="details-title">
    <button className="back-link" type="button" onClick={onBack}>← Back to request history</button>
    <p className="eyebrow">Request history</p>
    <h1 id="details-title" ref={headingRef} tabIndex={-1}>Credit register extract</h1>
    {loading && <div className="loading-message" role="status" aria-live="polite"><span className="spinner" aria-hidden="true" /><span>Loading credit register extract…</span></div>}
    {error && <div className="error-panel" ref={errorRef} tabIndex={-1} role="alert"><strong>{error.title}</strong><p>{error.detail}</p>{error.reference && <span>Support reference: {error.reference}</span>}{!error.notFound && <p><button className="secondary-button" type="button" onClick={() => { setError(null); setLoading(true); setRetry((value) => value + 1) }}>Retry</button></p>}</div>}
    {extract && summary && <>
      <p className="masked-code">{details.consumer.maskedPersonalIdentityCode}</p><p className="snapshot-label">Immutable register snapshot</p>
      <div className={extract.voluntaryBanOnCredits.isInEffect ? 'ban-banner active-ban' : 'ban-banner'} role={extract.voluntaryBanOnCredits.isInEffect ? 'alert' : undefined}><span aria-hidden="true">{extract.voluntaryBanOnCredits.isInEffect ? '⚠' : '✓'}</span><div><strong>{extract.voluntaryBanOnCredits.isInEffect ? 'Active voluntary credit ban' : 'No active voluntary credit ban reported'}</strong>{extract.voluntaryBanOnCredits.reason && <p>Reason: {label(extract.voluntaryBanOnCredits.reason)}</p>}</div></div>
      <section className="detail-section"><h2>Provenance</h2><dl className="result-grid detail-grid"><Value label="Source">Finnish Positive Credit Register</Value><Value label="Extract reference"><span title={extract.extractReference}>{extract.extractReference}</span></Value><Value label="Extract created">{instant(extract.creationTimeUtc, true)}</Value><Value label="Requested">{instant(details.requestedAt)}</Value><Value label="Completed">{instant(details.completedAt)}</Value><Value label="Purpose">{details.creditRegisterExtractPurposes.map(label).join(', ')}</Value><Value label="Snapshot">Immutable</Value></dl></section>
      <section className="detail-section"><h2>Summary</h2><dl className="result-grid detail-grid"><Value label="Lenders">{summary.lendersCount}</Value><Value label="Loan contracts">{summary.loanContractsCount}</Value><Value label="Guaranteed loan contracts">{summary.guaranteedLoanContractsCount}</Value></dl></section>
      <details className="detail-section"><summary>Repayments &amp; leasing</summary><dl className="result-grid detail-grid"><Value label="Repayments paid last amount">{currencyAmounts(summary.repaymentsPaidLastAmount)}</Value><Value label="Monthly leasing instalments">{currencyAmounts(summary.sumOfMonthlyLeasingInstalments)}</Value></dl></details>
      <details className="detail-section"><summary>Loans ({extract.loans.length})</summary>{extract.loans.length ? extract.loans.map((loan, index) => <LoanDetails key={index} loan={loan} index={index} />) : <p className="empty-inline">No loans were reported.</p>}</details>
      <details className="detail-section"><summary>Delayed amounts ({delayed.length})</summary>{delayed.length ? <table><caption>Reported delayed amounts by loan</caption><thead><tr><th>Loan</th><th>Delayed instalment</th><th>Original due date</th><th>Foreclosed</th></tr></thead><tbody>{delayed.map((entry, index) => <tr key={index}><td data-label="Loan">Loan {entry.loanIndex + 1} — {label(entry.loanType)}</td><td data-label="Delayed instalment">{amount(entry.delayedInstalment, entry.currencyCode)}</td><td data-label="Original due date">{date(entry.originalDueDate)}</td><td data-label="Foreclosed">{entry.isForeclosed ? 'Yes' : 'No'}</td></tr>)}</tbody></table> : <p className="empty-inline">No delayed amounts were reported.</p>}</details>
      <details className="detail-section"><summary>Income ({extract.incomeData.reduce((total, item) => total + item.months.length, 0)} months)</summary>{extract.incomeData.length ? extract.incomeData.map((income) => <section key={income.year}><h3>{income.year}</h3>{income.months.length ? <table><caption>Income amounts (EUR)</caption><thead><tr><th>Month</th><th>Wages gross</th><th>Wages net</th><th>Benefits gross</th><th>Benefits net</th></tr></thead><tbody>{income.months.map((month) => <tr key={month.month}><td data-label="Month">{month.month}</td><td data-label="Wages gross">{incomeAmount(month.wagesGrossAmount)}</td><td data-label="Wages net">{incomeAmount(month.wagesNetAmount)}</td><td data-label="Benefits gross">{incomeAmount(month.benefitsGrossAmount)}</td><td data-label="Benefits net">{incomeAmount(month.benefitsNetAmount)}</td></tr>)}</tbody></table> : <p className="empty-inline">No income months were reported for this year.</p>}</section>) : <p className="empty-inline">No income data were reported.</p>}</details>
    </>}
  </section>
}
