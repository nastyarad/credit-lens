export const CREDIT_REGISTER_EXTRACT_PURPOSES = [
  'NewConsumerCredit',
  'IncreaseOfConsumerCreditPrincipalOrCreditLimit',
  'ChangesToTermsOfConsumerCredit',
  'GuaranteeOrThirdPartyPledgeForConsumerCredit',
  'NewLoan',
  'IncreaseOfLoanPrincipalOrCreditLimit',
  'ChangesToTerms',
  'GuaranteeOrThirdPartyPledge',
] as const

export type CreditRegisterExtractPurpose =
  (typeof CREDIT_REGISTER_EXTRACT_PURPOSES)[number]

export type FinancingRequestStatus = 'COMPLETED' | string

export interface CreateFinancingRequestPayload {
  clientRequestId: string
  personalIdentityCode: string
  creditRegisterExtractPurposes: CreditRegisterExtractPurpose[]
}

export interface ApiProblem {
  type?: string
  title?: string
  status?: number
  detail?: string
  instance?: string
  correlationId?: string
}

export interface Consumer {
  id: string
  maskedPersonalIdentityCode: string
}

export interface VoluntaryBanOnCredits {
  isInEffect: boolean
  reason: string | null
}

export interface CreditExtractSummary {
  extractReference: string
  creationTimeUtc?: string
  voluntaryBanOnCredits: VoluntaryBanOnCredits
  lendersCount: number
  loanContractsCount: number
  guaranteedLoanContractsCount: number
}

export interface FinancingRequestResponse {
  id: string
  clientRequestId: string
  consumer: Consumer
  creditRegisterExtractPurposes: CreditRegisterExtractPurpose[]
  status?: FinancingRequestStatus
  requestedAt: string
  completedAt: string
  error?: ApiProblem | null
  creditExtractSummary: CreditExtractSummary
}

