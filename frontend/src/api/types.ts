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

export type VoluntaryCreditBanReason =
  | 'RiskOfIdentityTheft'
  | 'ControlOfPersonalFinances'
  | 'Other'

export interface VoluntaryBanOnCredits {
  isInEffect: boolean
  reason: VoluntaryCreditBanReason | null
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

export interface CurrencyAmount {
  currencyCode: string
  sum: number
}

export interface PaymentPlan {
  isInDebtArrangement: boolean
  isInBusinessRestructuringProgram: boolean
}

export interface LumpSumLoan {
  amountIssued: number | null
  amountPaid: number | null
  balance: number | null
  plannedFinalDueDate: string | null
  amortizationFrequency: number | null
}

export interface RunningAccountLoan {
  creditLimit: number | null
  balance: number | null
  balanceDate: string | null
}

export interface LeasingContract {
  contractPeriodStartDate: string | null
  transactionPrice: number | null
}

export interface DelayedAmount {
  delayedInstalment: number | null
  originalDueDate: string | null
  isForeclosed: boolean
}

export interface Loan {
  loanType: string
  contractDate: string | null
  isLoanWithCollateral: boolean | null
  collateralType: string[]
  borrowersCount: number | null
  currencyCode: string | null
  paymentPlan: PaymentPlan | null
  accuracyIsDenied: boolean
  lumpSumLoan: LumpSumLoan | null
  runningAccountLoan: RunningAccountLoan | null
  leasingContract: LeasingContract | null
  delayedAmount: DelayedAmount[]
}

export interface MonthlyIncome {
  month: number
  wagesGrossAmount: number | null
  wagesNetAmount: number | null
  benefitsGrossAmount: number | null
  benefitsNetAmount: number | null
}

export interface IncomeData {
  year: number
  months: MonthlyIncome[]
}

export interface CreditInformationSummary {
  lendersCount: number
  loanContractsCount: number
  guaranteedLoanContractsCount: number
  repaymentsPaidLastAmount: CurrencyAmount[]
  sumOfMonthlyLeasingInstalments: CurrencyAmount[]
}

export interface CreditExtract {
  extractReference: string
  creationTimeUtc: string
  voluntaryBanOnCredits: VoluntaryBanOnCredits
  creditInformationSummary: CreditInformationSummary
  loans: Loan[]
  incomeData: IncomeData[]
}

export interface FinancingRequestDetails {
  id: string
  clientRequestId: string
  consumer: Consumer
  creditRegisterExtractPurposes: CreditRegisterExtractPurpose[]
  requestedAt: string
  completedAt: string
  creditExtract: CreditExtract
}
