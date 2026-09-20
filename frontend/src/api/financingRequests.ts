import type {
  ApiProblem,
  CreateFinancingRequestPayload,
  FinancingRequestResponse,
  FinancingRequestHistoryPage,
  FinancingRequestSearchPayload,
} from './types'

export class FinancingRequestApiError extends Error {
  readonly kind = 'api' as const
  readonly status: number
  readonly problem: ApiProblem

  constructor(status: number, problem: ApiProblem) {
    super(problem.title || 'Financing request failed')
    this.name = 'FinancingRequestApiError'
    this.status = status
    this.problem = problem
  }
}

export class FinancingRequestNetworkError extends Error {
  readonly kind = 'network' as const

  constructor() {
    super('The financing request could not reach Credit Lens.')
    this.name = 'FinancingRequestNetworkError'
  }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

function parseApiProblem(value: unknown, status: number): ApiProblem {
  if (!isRecord(value)) {
    return {
      title: status === 409 ? 'Request already exists' : 'Request could not be completed',
      detail: 'Credit Lens returned an unexpected error response.',
      status,
    }
  }

  const problem: ApiProblem = {}
  if (typeof value.type === 'string') problem.type = value.type
  if (typeof value.title === 'string') problem.title = value.title
  if (typeof value.status === 'number') problem.status = value.status
  if (typeof value.detail === 'string') problem.detail = value.detail
  if (typeof value.instance === 'string') problem.instance = value.instance
  if (typeof value.correlationId === 'string') problem.correlationId = value.correlationId
  return problem
}

export async function createFinancingRequest(
  payload: CreateFinancingRequestPayload,
  signal?: AbortSignal,
): Promise<FinancingRequestResponse> {
  let response: Response
  try {
    response = await fetch('/api/v1/financing-requests', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
      body: JSON.stringify(payload),
      signal,
    })
  } catch {
    throw new FinancingRequestNetworkError()
  }

  if (!response.ok) {
    let body: unknown = null
    try {
      body = await response.json()
    } catch {
      // Do not expose raw response text to the user.
    }
    throw new FinancingRequestApiError(response.status, parseApiProblem(body, response.status))
  }

  try {
    return (await response.json()) as FinancingRequestResponse
  } catch {
    throw new FinancingRequestApiError(response.status, {
      title: 'Invalid server response',
      detail: 'Credit Lens returned a response that could not be read.',
      status: response.status,
    })
  }
}

export async function searchFinancingRequests(
  payload: FinancingRequestSearchPayload,
  signal?: AbortSignal,
): Promise<FinancingRequestHistoryPage> {
  let response: Response
  try {
    response = await fetch('/api/v1/financing-requests/search', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
      body: JSON.stringify(payload),
      signal,
    })
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') throw error
    throw new FinancingRequestNetworkError()
  }

  if (!response.ok) {
    let body: unknown = null
    try { body = await response.json() } catch { /* Keep server response private. */ }
    throw new FinancingRequestApiError(response.status, parseApiProblem(body, response.status))
  }

  try {
    return (await response.json()) as FinancingRequestHistoryPage
  } catch {
    throw new FinancingRequestApiError(response.status, {
      title: 'Invalid server response',
      detail: 'Credit Lens returned a response that could not be read.',
      status: response.status,
    })
  }
}
