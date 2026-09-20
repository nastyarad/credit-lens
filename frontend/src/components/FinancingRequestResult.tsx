import type { FinancingRequestResponse } from '../api/types'
import { purposeLabels } from './purposeLabels'

interface FinancingRequestResultProps {
  result: FinancingRequestResponse
  onCreateAnother: () => void
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}

export function FinancingRequestResult({ result, onCreateAnother }: FinancingRequestResultProps) {
  const summary = result.creditExtractSummary
  const banLabel = summary.voluntaryBanOnCredits.isInEffect ? 'Active' : 'Not active'

  return (
    <section className="result-card" aria-labelledby="result-title">
      <div className="result-heading">
        <div>
          <p className="eyebrow">Request completed</p>
          <h2 id="result-title">Credit extract is ready</h2>
        </div>
      </div>
      <dl className="result-grid">
        <div><dt>Identity code</dt><dd>{result.consumer.maskedPersonalIdentityCode}</dd></div>
        <div><dt>Requested</dt><dd>{formatDate(result.requestedAt)}</dd></div>
        <div><dt>Completed</dt><dd>{formatDate(result.completedAt)}</dd></div>
        <div><dt>Extract reference</dt><dd className="reference-value">{summary.extractReference}</dd></div>
        <div><dt>Voluntary credit ban</dt><dd>{banLabel}</dd></div>
        <div><dt>Lenders</dt><dd>{summary.lendersCount}</dd></div>
        <div><dt>Loan contracts</dt><dd>{summary.loanContractsCount}</dd></div>
        <div><dt>Guaranteed loan contracts</dt><dd>{summary.guaranteedLoanContractsCount}</dd></div>
      </dl>
      <dl className="purpose-summary">
        <dt>Purposes</dt>
        <dd>{result.creditRegisterExtractPurposes.map((purpose) => purposeLabels[purpose] || purpose).join(', ')}</dd>
      </dl>
      <button className="secondary-button" type="button" onClick={onCreateAnother}>Create another request</button>
    </section>
  )
}
