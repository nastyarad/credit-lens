import type {
  CreditRegisterExtractPurpose,
  VoluntaryCreditBanReason,
} from '../api/types'

export const purposeLabels: Record<CreditRegisterExtractPurpose, string> = {
  NewConsumerCredit: 'New consumer credit',
  IncreaseOfConsumerCreditPrincipalOrCreditLimit: 'Increase of consumer credit principal or limit',
  ChangesToTermsOfConsumerCredit: 'Changes to terms of consumer credit',
  GuaranteeOrThirdPartyPledgeForConsumerCredit: 'Guarantee or third-party pledge for consumer credit',
  NewLoan: 'New loan',
  IncreaseOfLoanPrincipalOrCreditLimit: 'Increase of loan principal or limit',
  ChangesToTerms: 'Changes to terms',
  GuaranteeOrThirdPartyPledge: 'Guarantee or third-party pledge',
}

export const voluntaryCreditBanReasonLabels: Record<
  VoluntaryCreditBanReason,
  string
> = {
  RiskOfIdentityTheft: 'Risk of identity theft',
  ControlOfPersonalFinances: 'Control of personal finances',
  Other: 'Other reason',
}
