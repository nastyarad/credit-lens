import { useId, useState } from 'react'
import type { FormEvent } from 'react'
import {
  CREDIT_REGISTER_EXTRACT_PURPOSES,
  type CreditRegisterExtractPurpose,
} from '../api/types'
import { purposeLabels } from './purposeLabels'
const PERSONAL_IDENTITY_CODE_PATTERN = /^[0-9]{6}[+\-A-FYXWVU][0-9]{3}[0-9A-FHJ-NPR-Y]$/

interface FinancingRequestFormProps {
  disabled: boolean
  onSubmit: (personalIdentityCode: string, purpose: CreditRegisterExtractPurpose) => void
}

export function FinancingRequestForm({ disabled, onSubmit }: FinancingRequestFormProps) {
  const identityInputId = useId()
  const purposeInputId = useId()
  const identityErrorId = `${identityInputId}-error`
  const [personalIdentityCode, setPersonalIdentityCode] = useState('')
  const [purpose, setPurpose] = useState<CreditRegisterExtractPurpose>('NewConsumerCredit')
  const [validationError, setValidationError] = useState('')

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const trimmedCode = personalIdentityCode.trim().toUpperCase()
    if (!trimmedCode) {
      setValidationError('Enter a Finnish personal identity code.')
      return
    }
    if (!PERSONAL_IDENTITY_CODE_PATTERN.test(trimmedCode)) {
      setValidationError('Enter a valid Finnish personal identity code.')
      return
    }
    setValidationError('')
    onSubmit(trimmedCode, purpose)
  }

  return (
    <form className="request-form" onSubmit={handleSubmit} noValidate>
      <div className="field-group">
        <label htmlFor={identityInputId}>Finnish personal identity code</label>
        <input
          id={identityInputId}
          name="personalIdentityCode"
          type="text"
          autoComplete="off"
          inputMode="text"
          placeholder="010190-123A"
          value={personalIdentityCode}
          onChange={(event) => {
            setPersonalIdentityCode(event.target.value)
            if (validationError) setValidationError('')
          }}
          aria-invalid={Boolean(validationError)}
          aria-describedby={validationError ? identityErrorId : undefined}
          disabled={disabled}
        />
        {validationError && (
          <p className="field-error" id={identityErrorId} role="alert">
            {validationError}
          </p>
        )}
      </div>

      <div className="field-group">
        <label htmlFor={purposeInputId}>Purpose of the credit register extract</label>
        <select
          id={purposeInputId}
          name="purpose"
          value={purpose}
          onChange={(event) => setPurpose(event.target.value as CreditRegisterExtractPurpose)}
          disabled={disabled}
        >
          {CREDIT_REGISTER_EXTRACT_PURPOSES.map((value) => (
            <option key={value} value={value}>{purposeLabels[value]}</option>
          ))}
        </select>
      </div>

      <button className="primary-button" type="submit" disabled={disabled}>
        {disabled ? 'Creating request…' : 'Create financing request'}
      </button>
    </form>
  )
}
