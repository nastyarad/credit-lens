import { useEffect, useRef, useState } from "react";
import type { ReactNode } from "react";
import {
  ArrowRight,
  BadgeCheck,
  ChartNoAxesColumn,
  FilePlus2,
  Fingerprint,
  History,
  Plus,
  ScanLine,
  TriangleAlert,
} from "lucide-react";
import {
  createFinancingRequest,
  FinancingRequestApiError,
  FinancingRequestNetworkError,
} from "./api/financingRequests";
import type {
  CreditRegisterExtractPurpose,
  FinancingRequestResponse,
} from "./api/types";
import { FinancingRequestForm } from "./components/FinancingRequestForm";
import { FinancingRequestResult } from "./components/FinancingRequestResult";
import { FinancingRequestHistory } from "./components/FinancingRequestHistory";
import { FinancingRequestDetailsView } from "./components/FinancingRequestDetails";
import "./App.css";

type RequestState =
  | "idle"
  | "submitting"
  | "completed"
  | "api-error"
  | "network-error";
type Submission = {
  personalIdentityCode: string;
  purpose: CreditRegisterExtractPurpose;
};
type RequestError = {
  kind:
    | "rejection"
    | "timeout"
    | "unavailable"
    | "conflict"
    | "unexpected"
    | "network";
  title: string;
  detail: string;
  reference?: string;
  retryable: boolean;
};

function classifyError(error: unknown): RequestError {
  if (error instanceof FinancingRequestNetworkError)
    return {
      kind: "network",
      title: "The service is temporarily unavailable",
      detail:
        "Check your connection and try again. No successful request was saved.",
      retryable: true,
    };
  if (error instanceof FinancingRequestApiError) {
    if (error.status === 422)
      return {
        kind: "rejection",
        title: "We could not request an extract",
        detail:
          "Review the information and start a new request if needed. No successful request was saved.",
        reference: error.problem.correlationId,
        retryable: false,
      };
    if (error.status === 504)
      return {
        kind: "timeout",
        title: "The request timed out",
        detail:
          "Try again. No successful request was saved.",
        reference: error.problem.correlationId,
        retryable: true,
      };
    if (error.status === 502 || error.status === 503)
      return {
        kind: "unavailable",
        title: "The service is temporarily unavailable",
        detail:
          "Try again later. No successful request was saved.",
        reference: error.problem.correlationId,
        retryable: true,
      };
    if (error.status === 409)
      return {
        kind: "conflict",
        title: "We could not complete this request",
        detail: "Start a new request. No successful request was saved.",
        reference: error.problem.correlationId,
        retryable: false,
      };
    return {
      kind: "unexpected",
      title: "We could not complete the request",
      detail: "Try again. No successful request was saved.",
      reference: error.problem.correlationId,
      retryable: true,
    };
  }
  return {
    kind: "unexpected",
    title: "We could not complete the request",
    detail: "Try again. No successful request was saved.",
    retryable: true,
  };
}

function App() {
  const [section, setSection] = useState<"new" | "history">("new");
  const [detailsId, setDetailsId] = useState<string | null>(null);
  const [state, setState] = useState<RequestState>("idle");
  const [result, setResult] = useState<FinancingRequestResponse | null>(null);
  const [error, setError] = useState<RequestError | null>(null);
  const [clientRequestId, setClientRequestId] = useState<string | null>(null);
  const [lastSubmission, setLastSubmission] = useState<Submission | null>(null);
  const pageTitleRef = useRef<HTMLHeadingElement>(null);
  const isSubmitting = state === "submitting";
  const title =
    detailsId
      ? "Credit register extract"
      : section === "history"
      ? "Request history"
      : state === "completed"
        ? "Register extract received"
        : "Request a credit register extract";

  useEffect(() => {
    document.title = error
      ? `Error: ${title} | Credit Lens`
      : `${title} | Credit Lens`;
  }, [error, title]);
  function moveToSection(nextSection: "new" | "history") {
    setSection(nextSection);
    setDetailsId(null);
    if (nextSection === "new") {
      setResult(null);
      setError(null);
      setState("idle");
    }
  }
  async function submitRequest(
    personalIdentityCode: string,
    purpose: CreditRegisterExtractPurpose,
  ) {
    const canRetrySameRequest = Boolean(
      clientRequestId &&
        lastSubmission?.personalIdentityCode === personalIdentityCode &&
        lastSubmission.purpose === purpose,
    );
    const requestId = canRetrySameRequest
      ? clientRequestId!
      : crypto.randomUUID();
    setClientRequestId(requestId);
    setLastSubmission({ personalIdentityCode, purpose });
    setState("submitting");
    setError(null);
    try {
      const response = await createFinancingRequest({
        clientRequestId: requestId,
        personalIdentityCode,
        creditRegisterExtractPurposes: [purpose],
      });
      setResult(response);
      setState("completed");
      setClientRequestId(null);
      setLastSubmission(null);
      requestAnimationFrame(() => pageTitleRef.current?.focus());
    } catch (requestError: unknown) {
      const nextError = classifyError(requestError);
      setState(nextError.kind === "network" ? "network-error" : "api-error");
      setError(nextError);
      requestAnimationFrame(() => pageTitleRef.current?.focus());
    }
  }
  function startNewRequest() {
    setResult(null);
    setError(null);
    setClientRequestId(null);
    setLastSubmission(null);
    setState("idle");
    setSection("new");
    requestAnimationFrame(() => pageTitleRef.current?.focus());
  }
  function retryRequest() {
    if (lastSubmission)
      void submitRequest(
        lastSubmission.personalIdentityCode,
        lastSubmission.purpose,
      );
  }
  function openDetails(id: string) {
    setSection("history");
    setDetailsId(id);
  }
  function returnToHistory() {
    setDetailsId(null);
    requestAnimationFrame(() => pageTitleRef.current?.focus());
  }
  return (
    <div className="app-shell">
      <a className="skip-link" href="#main-content">
        Skip to main content
      </a>
      <aside className="sidebar" aria-label="Credit Lens navigation">
        <div className="brand">
          <span className="brand-mark" aria-hidden="true">
            <ScanLine />
          </span>
          <span>Credit Lens</span>
        </div>
        <p className="product-label">PCR workspace</p>
        <nav className="side-nav" aria-label="Main navigation">
          <button
            className={section === "new" ? "nav-button active" : "nav-button"}
            type="button"
            aria-current={section === "new" ? "page" : undefined}
            onClick={() => moveToSection("new")}
          >
            <FilePlus2 aria-hidden="true" />
            <span>New request</span>
          </button>
          <button
            className={
              section === "history" ? "nav-button active" : "nav-button"
            }
            type="button"
            aria-current={section === "history" ? "page" : undefined}
            onClick={() => moveToSection("history")}
          >
            <History aria-hidden="true" />
            <span>Request history</span>
          </button>
        </nav>
        <div className="side-note">
          <strong>Evidence, not decisions</strong>Register facts support the
          lender’s own assessment process.
        </div>
      </aside>
      <div className="app-main">
        <header className="topbar">
          <div className="mobile-brand">
            <ScanLine aria-hidden="true" />
            <span>Credit Lens</span>
          </div>
          <div className="crumbs">
            Lending operations&nbsp; / &nbsp;
            <strong>Positive Credit Register</strong>
          </div>
          <div className="source">
            <BadgeCheck aria-hidden="true" />
            <span>Finnish register source</span>
          </div>
        </header>
        <div className="mobile-nav" aria-label="Section navigation">
          <button
            type="button"
            className={section === "new" ? "active" : ""}
            onClick={() => moveToSection("new")}
          >
            <FilePlus2 aria-hidden="true" />
            New request
          </button>
          <button
            type="button"
            className={section === "history" ? "active" : ""}
            onClick={() => moveToSection("history")}
          >
            <History aria-hidden="true" />
            Request history
          </button>
        </div>
        <main id="main-content" tabIndex={-1}>
          <div className="content">
            <div className="page-head">
              <div>
                <h1 ref={pageTitleRef} tabIndex={-1}>
                  {title}
                </h1>
                <p>
                  {detailsId ? (
                    "Review the immutable register snapshot for this financing request."
                  ) : section === "history" ? (
                    "Find successful register requests for one consumer. Failed requests are not stored."
                  ) : state === "completed" && result ? (
                    <>
                      Consumer{" "}
                      <strong>
                        {result.consumer.maskedPersonalIdentityCode}
                      </strong>{" "}
                      · Extract created{" "}
                      {new Intl.DateTimeFormat(undefined, {
                        dateStyle: "medium",
                        timeStyle: "short",
                      }).format(
                        new Date(
                          result.creditExtractSummary.creationTimeUtc ||
                            result.completedAt,
                        ),
                      )}
                    </>
                  ) : (
                    "Retrieve a current register snapshot for a consumer financing assessment."
                  )}
                </p>
              </div>
              {state === "completed" && result && section === "new" && (
                <button
                  className="secondary-button page-action"
                  type="button"
                  onClick={startNewRequest}
                >
                  <Plus aria-hidden="true" />
                  New request
                </button>
              )}
            </div>
            {detailsId ? (
              <FinancingRequestDetailsView id={detailsId} onBack={returnToHistory} />
            ) : section === "history" ? (
              <FinancingRequestHistory onViewDetails={openDetails} />
            ) : state === "completed" && result ? (
              <FinancingRequestResult result={result} />
            ) : (
              <>
                {error && (
                  <ErrorRecovery
                    error={error}
                    onRetry={retryRequest}
                    onStartNew={startNewRequest}
                  />
                )}
                {isSubmitting && (
                  <div
                    className="loading-message"
                    role="status"
                    aria-live="polite"
                  >
                    <span className="spinner" aria-hidden="true" />
                    <span>
                      Requesting extract from the Positive Credit Register
                    </span>
                  </div>
                )}
                <div className="request-layout">
                  <FinancingRequestForm
                    disabled={isSubmitting}
                    onSubmit={submitRequest}
                  />
                  <aside
                    className="context-panel"
                    aria-labelledby="context-title"
                  >
                    <div className="context-accent" aria-hidden="true" />
                    <div className="panel-body">
                      <h2 id="context-title">What you’ll receive</h2>
                      <ContextRow
                        icon={<TriangleAlert />}
                        title="Credit ban signal"
                      >
                        Displayed first when the register reports an active
                        voluntary ban.
                      </ContextRow>
                      <ContextRow
                        icon={<ChartNoAxesColumn />}
                        title="Credit summary"
                      >
                        Lender and loan-contract counts from the immutable
                        extract.
                      </ContextRow>
                      <ContextRow
                        icon={<Fingerprint />}
                        title="Source provenance"
                      >
                        Reference and timestamps for the register snapshot.
                      </ContextRow>
                    </div>
                  </aside>
                </div>
              </>
            )}
          </div>
        </main>
      </div>
    </div>
  );
}
function ContextRow({
  icon,
  title,
  children,
}: {
  icon: ReactNode;
  title: string;
  children: string;
}) {
  return (
    <div className="context-row">
      <span className="context-icon" aria-hidden="true">
        {icon}
      </span>
      <div>
        <strong>{title}</strong>
        <span>{children}</span>
      </div>
    </div>
  );
}
function ErrorRecovery({
  error,
  onRetry,
  onStartNew,
}: {
  error: RequestError;
  onRetry: () => void;
  onStartNew: () => void;
}) {
  return (
    <section
      className={`recovery-panel recovery-${error.kind}`}
      role="alert"
      aria-labelledby="recovery-title"
    >
      <div className="alert-icon" aria-hidden="true">
        <TriangleAlert />
      </div>
      <div>
        <p className="kicker">Request not completed</p>
        <h2 id="recovery-title">{error.title}</h2>
        <p>{error.detail}</p>
        {error.reference && (
          <p className="support-reference">
            Support reference: <code>{error.reference}</code>
          </p>
        )}
        <div className="action-row">
          {error.retryable && (
            <button className="primary-button" type="button" onClick={onRetry}>
              <ArrowRight aria-hidden="true" />
              Retry request
            </button>
          )}
          <button
            className="secondary-button"
            type="button"
            onClick={onStartNew}
          >
            Start a new request
          </button>
        </div>
      </div>
    </section>
  );
}
export default App;
