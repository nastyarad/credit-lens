import { useId, useRef, useState } from "react";
import type { FormEvent } from "react";
import { Search } from "lucide-react";
import {
  FinancingRequestApiError,
  FinancingRequestNetworkError,
  searchFinancingRequests,
} from "../api/financingRequests";
import type { FinancingRequestHistoryPage } from "../api/types";
import {
  normalizePersonalIdentityCode,
  validatePersonalIdentityCode,
} from "../identityCode";

function formatDate(value: string) {
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}
function safeError(error: unknown) {
  if (error instanceof FinancingRequestApiError)
    return {
      title: "We could not load request history",
      detail: "Try again. If the problem continues, contact support.",
      reference: error.problem.correlationId,
    };
  if (error instanceof FinancingRequestNetworkError)
    return {
      title: "We could not load request history",
      detail: "Check your connection and try again.",
    };
  return {
    title: "We could not load request history",
    detail: "Try again. If the problem continues, contact support.",
  };
}

export function FinancingRequestHistory({
  onViewDetails,
}: {
  onViewDetails: (id: string) => void;
}) {
  const inputId = useId();
  const errorId = `${inputId}-error`;
  const inputRef = useRef<HTMLInputElement>(null);
  const errorRef = useRef<HTMLDivElement>(null);
  const [value, setValue] = useState("");
  const [submittedCode, setSubmittedCode] = useState<string | null>(null);
  const [page, setPage] = useState<FinancingRequestHistoryPage | null>(null);
  const [loading, setLoading] = useState(false);
  const [validationError, setValidationError] = useState("");
  const [error, setError] = useState<{
    title: string;
    detail: string;
    reference?: string;
  } | null>(null);
  async function loadHistory(code: string, pageNumber: number) {
    setLoading(true);
    setError(null);
    try {
      const result = await searchFinancingRequests({
        personalIdentityCode: code,
        page: pageNumber,
        size: 20,
      });
      setPage(result);
      setSubmittedCode(code);
      setValue("");
    } catch (requestError) {
      const nextError = safeError(requestError);
      setError(nextError);
      requestAnimationFrame(() => errorRef.current?.focus());
    } finally {
      setLoading(false);
    }
  }
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const code = normalizePersonalIdentityCode(value);
    if (validatePersonalIdentityCode(code)) {
      setValidationError(
        "Enter a Finnish personal identity code in the required format.",
      );
      requestAnimationFrame(() => inputRef.current?.focus());
      return;
    }
    setValidationError("");
    void loadHistory(code, 0);
  }
  const totalPages = page?.totalPages ?? 0;
  const currentPage = page && totalPages > 0 ? page.page + 1 : 0;
  const canGoPrevious = Boolean(page && page.page > 0);
  const canGoNext = Boolean(page && page.page + 1 < totalPages);
  return (
    <section className="history-view" aria-labelledby="history-results-title">
      <form className="history-search" onSubmit={submit} noValidate>
        <div className="field-group">
          <label htmlFor={inputId}>Finnish personal identity code</label>
          <input
            ref={inputRef}
            id={inputId}
            type="text"
            autoComplete="off"
            inputMode="text"
            placeholder="DDMMYYCZZZQ"
            value={value}
            onChange={(event) => {
              setValue(event.target.value);
              setValidationError("");
            }}
            aria-invalid={Boolean(validationError)}
            aria-describedby={validationError ? errorId : undefined}
            disabled={loading}
          />
          {validationError && (
            <p className="field-error" id={errorId} role="alert">
              {validationError}
            </p>
          )}
        </div>
        <button
          className="primary-button"
          type="submit"
          aria-label="Search request history"
          disabled={loading}
        >
          <Search aria-hidden="true" />
          {loading ? "Searching…" : "Search history"}
        </button>
      </form>
      {loading && (
        <div className="loading-message" role="status" aria-live="polite">
          <span className="spinner" aria-hidden="true" />
          Loading request history…
        </div>
      )}
      {error && (
        <div
          className="error-panel"
          ref={errorRef}
          tabIndex={-1}
          role="alert"
        >
          <h2>{error.title}</h2>
          <p>{error.detail}</p>
          {error.reference && (
            <span>
              Support reference: <code>{error.reference}</code>
            </span>
          )}
        </div>
      )}
      {page && !loading && page.items.length === 0 && (
        <div className="empty-state">
          <h2>No successful requests found for this consumer.</h2>
          <p>A request that was rejected or failed is not shown here.</p>
        </div>
      )}
      {page && page.items.length > 0 && (
        <section
          className="panel history-panel"
          aria-labelledby="history-results-title"
        >
          <div className="history-meta">
            <strong id="history-results-title">
              Successful requests for {page.items[0].maskedPersonalIdentityCode}
            </strong>
            <span>{page.totalItems} extracts · newest first</span>
          </div>
          <div className="table-wrap">
            <table>
              <caption className="visually-hidden">
                Successful request history
              </caption>
              <thead>
                <tr>
                  <th>Requested</th>
                  <th>Voluntary ban</th>
                  <th>Details</th>
                </tr>
              </thead>
              <tbody>
                {page.items.map((item) => (
                  <tr key={item.id}>
                    <td>
                      <span className="mobile-label">Requested</span>
                      <time>{formatDate(item.requestedAt)}</time>
                    </td>
                    <td
                      className={
                        item.voluntaryCreditBanActive ? "ban-cell" : ""
                      }
                      data-field="ban"
                    >
                      <span className="mobile-label">Voluntary ban</span>
                      <span className="status">
                        <span className="status-dot" aria-hidden="true" />
                        {item.voluntaryCreditBanActive
                          ? "Active"
                          : "None reported"}
                      </span>
                    </td>
                    <td>
                      <button
                        className="secondary-button details-button"
                        type="button"
                        onClick={() => onViewDetails(item.id)}
                        aria-label={`View details for extract ${item.extractReference}`}
                      >
                        View details
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}
      {page && page.items.length > 0 && (
        <nav className="pagination" aria-label="Request history pages">
          <button
            className="secondary-button"
            type="button"
            disabled={loading || !canGoPrevious}
            onClick={() =>
              submittedCode &&
              page &&
              void loadHistory(submittedCode, page.page - 1)
            }
          >
            Previous
          </button>
          <span>
            Page {currentPage} of {totalPages}
          </span>
          <button
            className="secondary-button"
            type="button"
            disabled={loading || !canGoNext}
            onClick={() =>
              submittedCode &&
              page &&
              void loadHistory(submittedCode, page.page + 1)
            }
          >
            Next
          </button>
        </nav>
      )}
    </section>
  );
}
