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

export interface CreateFinancingRequestPayload {
  clientRequestId: string
  personalIdentityCode: string
  creditRegisterExtractPurposes: CreditRegisterExtractPurpose[]
}

export interface FinancingRequestSearchPayload {
  personalIdentityCode: string
  page: number
  size: number
}

export interface FinancingRequestHistoryItem {
  id: string
  clientRequestId: string
  maskedPersonalIdentityCode: string
  requestedAt: string
  completedAt: string
  extractReference: string
  voluntaryCreditBanActive: boolean
}

export interface FinancingRequestHistoryPage {
  items: FinancingRequestHistoryItem[]
  page: number
  size: number
  totalItems: number
  totalPages: number
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
  requestedAt: string
  completedAt: string
  creditExtractSummary: CreditExtractSummary
}
