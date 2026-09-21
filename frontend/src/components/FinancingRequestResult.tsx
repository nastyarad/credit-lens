import { ShieldAlert } from "lucide-react";
import type { FinancingRequestResponse } from "../api/types";
import { purposeLabels } from "./purposeLabels";

interface Props {
  result: FinancingRequestResponse;
}
function formatDate(value?: string) {
  return value
    ? new Intl.DateTimeFormat(undefined, {
        dateStyle: "medium",
        timeStyle: "short",
      }).format(new Date(value))
    : "Not provided";
}

export function FinancingRequestResult({ result }: Props) {
  const summary = result.creditExtractSummary;
  const active = summary.voluntaryBanOnCredits.isInEffect;
  return (
    <div className="result-view">
      <section
        className={active ? "ban-notice active" : "ban-notice"}
        role={active ? "alert" : undefined}
        aria-labelledby="ban-title"
      >
        <span className="ban-icon">
          <ShieldAlert aria-hidden="true" />
        </span>
        <div>
          <p className="kicker">
            {active ? "Attention required" : "Register information"}
          </p>
          <h2 id="ban-title">
            {active
              ? "Active voluntary credit ban"
              : "No active voluntary credit ban reported"}
          </h2>
          <p>
            {active
              ? "The Positive Credit Register reports an active voluntary credit ban. Apply the lender’s required care process. Credit Lens does not make a financing decision."
              : "The register does not report an active voluntary credit ban. This screen shows register facts and does not make a financing decision."}
          </p>
        </div>
        {active && summary.voluntaryBanOnCredits.reason && (
          <div className="ban-reason">
            <span>Register reason</span>
            <strong>{summary.voluntaryBanOnCredits.reason}</strong>
          </div>
        )}
      </section>
      <section className="panel summary-panel" aria-labelledby="summary-title">
        <div className="panel-head">
          <h2 id="summary-title">Credit summary</h2>
          <p>Facts reported in this register snapshot.</p>
        </div>
        <dl className="metrics">
          <div>
            <dt>Lenders</dt>
            <dd>{summary.lendersCount}</dd>
          </div>
          <div>
            <dt>Loan contracts</dt>
            <dd>{summary.loanContractsCount}</dd>
          </div>
          <div>
            <dt>Guaranteed contracts</dt>
            <dd>{summary.guaranteedLoanContractsCount}</dd>
          </div>
        </dl>
      </section>
      <section
        className="panel provenance-panel"
        aria-labelledby="provenance-title"
      >
        <div className="panel-head">
          <h2 id="provenance-title">Extract provenance</h2>
          <p>Immutable reference information for this request.</p>
        </div>
        <dl className="detail-grid">
          <Detail label="Source">Finnish Positive Credit Register</Detail>
          <Detail label="Extract reference" className="reference-value">
            {summary.extractReference}
          </Detail>
          <Detail label="Requested">{formatDate(result.requestedAt)}</Detail>
          <Detail label="Completed">{formatDate(result.completedAt)}</Detail>
          <Detail label="Purpose">
            {result.creditRegisterExtractPurposes
              .map((purpose) => purposeLabels[purpose] || purpose)
              .join(", ")}
          </Detail>
          <Detail label="Snapshot">Immutable</Detail>
        </dl>
      </section>
    </div>
  );
}
function Detail({
  label,
  children,
  className = "",
}: {
  label: string;
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <div className={className}>
      <dt>{label}</dt>
      <dd>{children}</dd>
    </div>
  );
}
