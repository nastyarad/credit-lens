import { useId, useRef, useState } from "react";
import type { FormEvent } from "react";
import { Info, ArrowRight, Shield } from "lucide-react";
import {
  CREDIT_REGISTER_EXTRACT_PURPOSES,
  type CreditRegisterExtractPurpose,
} from "../api/types";
import { purposeLabels } from "./purposeLabels";
import {
  normalizePersonalIdentityCode,
  validatePersonalIdentityCode,
} from "../identityCode";

interface Props {
  disabled: boolean;
  onSubmit: (
    personalIdentityCode: string,
    purpose: CreditRegisterExtractPurpose,
  ) => void;
}

export function FinancingRequestForm({
  disabled,
  onSubmit,
}: Props) {
  const identityInputId = useId();
  const purposeInputId = useId();
  const identityErrorId = `${identityInputId}-error`;
  const identityRef = useRef<HTMLInputElement>(null);
  const [personalIdentityCode, setPersonalIdentityCode] = useState("");
  const [purpose, setPurpose] =
    useState<CreditRegisterExtractPurpose>("NewConsumerCredit");
  const [validationError, setValidationError] = useState("");
  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const normalized = normalizePersonalIdentityCode(personalIdentityCode);
    if (validatePersonalIdentityCode(normalized)) {
      setValidationError(
        "Enter a Finnish personal identity code in the required format.",
      );
      requestAnimationFrame(() => identityRef.current?.focus());
      return;
    }
    setValidationError("");
    setPersonalIdentityCode("");
    onSubmit(normalized, purpose);
  }
  function clearForm() {
    setPersonalIdentityCode("");
    setValidationError("");
    identityRef.current?.focus();
  }
  return (
    <section className="panel request-panel">
      <div className="panel-head">
        <h2>Consumer and purpose</h2>
        <p>Both fields are required to request an extract.</p>
      </div>
      <form
        className="panel-body request-form"
        onSubmit={handleSubmit}
        noValidate
      >
        <div className="field-group">
          <label htmlFor={identityInputId}>
            Finnish personal identity code
          </label>
          <input
            ref={identityRef}
            id={identityInputId}
            name="personalIdentityCode"
            type="text"
            autoComplete="off"
            inputMode="text"
            placeholder="DDMMYYCZZZQ"
            value={personalIdentityCode}
            onChange={(event) => {
              setPersonalIdentityCode(event.target.value);
              if (validationError) setValidationError("");
            }}
            aria-invalid={Boolean(validationError)}
            aria-describedby={validationError ? identityErrorId : undefined}
            disabled={disabled}
          />
          <p className="field-hint">
            <Shield aria-hidden="true" />
            Used only for this request. Results display a masked value.
          </p>
          {validationError && (
            <p className="field-error" id={identityErrorId} role="alert">
              {validationError}
            </p>
          )}
        </div>
        <div className="field-group">
          <label htmlFor={purposeInputId}>Purpose of the extract</label>
          <select
            id={purposeInputId}
            name="purpose"
            value={purpose}
            onChange={(event) =>
              setPurpose(event.target.value as CreditRegisterExtractPurpose)
            }
            disabled={disabled}
          >
            {CREDIT_REGISTER_EXTRACT_PURPOSES.map((value) => (
              <option key={value} value={value}>
                {purposeLabels[value]}
              </option>
            ))}
          </select>
          <p className="field-hint">
            <Info aria-hidden="true" />
            Select the purpose that matches the current lending activity.
          </p>
        </div>
        <div className="form-actions">
          <span className="form-note">The request is sent synchronously.</span>
          <div className="action-row">
            <button
              className="primary-button"
              type="submit"
              disabled={disabled}
            >
              <ArrowRight aria-hidden="true" />
              {disabled ? "Requesting extract…" : "Request extract"}
            </button>
            <button
              className="secondary-button"
              type="button"
              onClick={clearForm}
              disabled={disabled || !personalIdentityCode}
            >
              Clear form
            </button>
          </div>
        </div>
      </form>
    </section>
  );
}
