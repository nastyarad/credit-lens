import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import App from './App'

const responseBody = {
  id: 'request-id',
  clientRequestId: 'client-id',
  consumer: { id: 'consumer-id', maskedPersonalIdentityCode: '******-123A' },
  creditRegisterExtractPurposes: ['NewConsumerCredit'],
  requestedAt: '2026-09-18T10:15:29Z',
  completedAt: '2026-09-18T10:15:30Z',
  creditExtractSummary: {
    extractReference: 'extract-reference',
    creationTimeUtc: '2026-09-18T10:15:30Z',
    voluntaryBanOnCredits: { isInEffect: false, reason: null },
    lendersCount: 0,
    loanContractsCount: 0,
    guaranteedLoanContractsCount: 0,
  },
}

function problemResponse(status: number) {
  return new Response(JSON.stringify({
    title: 'Client request ID conflict',
    detail: 'That request already exists.',
    correlationId: 'correlation-reference',
  }), { status, headers: { 'Content-Type': 'application/problem+json' } })
}

describe('financing request flow', () => {
  beforeEach(() => {
    vi.stubGlobal('crypto', { randomUUID: vi.fn(() => 'generated-client-id') })
  })

  afterEach(() => {
    cleanup()
    vi.restoreAllMocks()
    vi.unstubAllGlobals()
  })

  it('validates the identity code before sending a request', async () => {
    const user = userEvent.setup()
    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
    render(<App />)

    await user.click(screen.getByRole('button', { name: /create financing request/i }))

    expect(screen.getByText('Enter a Finnish personal identity code.')).toBeInTheDocument()
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('submits a new UUID and displays the completed response without the full identity code', async () => {
    const user = userEvent.setup()
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify(responseBody), { status: 201 }))
    vi.stubGlobal('fetch', fetchMock)
    render(<App />)

    await user.type(screen.getByLabelText(/personal identity code/i), '010190-123A')
    await user.click(screen.getByRole('button', { name: /create financing request/i }))

    await screen.findByRole('heading', { name: /credit extract is ready/i })
    expect(fetchMock).toHaveBeenCalledWith('/api/v1/financing-requests', expect.objectContaining({
      body: JSON.stringify({
        clientRequestId: 'generated-client-id',
        personalIdentityCode: '010190-123A',
        creditRegisterExtractPurposes: ['NewConsumerCredit'],
      }),
    }))
    expect(screen.getByText('******-123A')).toBeInTheDocument()
    expect(screen.queryByText('010190-123A')).not.toBeInTheDocument()
    expect(screen.getByText('extract-reference')).toBeInTheDocument()
  })

  it('shows safe API problem details and reuses the client ID on a technical retry', async () => {
    const user = userEvent.setup()
    const fetchMock = vi.fn().mockResolvedValue(problemResponse(409))
    vi.stubGlobal('fetch', fetchMock)
    render(<App />)
    await user.type(screen.getByLabelText(/personal identity code/i), '010190-123A')
    await user.click(screen.getByRole('button', { name: /create financing request/i }))
    await screen.findByRole('alert')
    expect(screen.getByText('That request already exists.')).toBeInTheDocument()
    expect(screen.getByText('Reference: correlation-reference')).toBeInTheDocument()
    expect(screen.queryByText(/raw response/i)).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /create financing request/i }))
    expect(fetchMock).toHaveBeenCalledTimes(2)
    const firstPayload = JSON.parse(fetchMock.mock.calls[0][1].body as string) as { clientRequestId: string }
    const secondPayload = JSON.parse(fetchMock.mock.calls[1][1].body as string) as { clientRequestId: string }
    expect(secondPayload.clientRequestId).toBe(firstPayload.clientRequestId)
  })

  it('shows a network error without exposing sensitive input', async () => {
    const user = userEvent.setup()
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')))
    render(<App />)
    await user.type(screen.getByLabelText(/personal identity code/i), '010190-123A')
    await user.click(screen.getByRole('button', { name: /create financing request/i }))

    await waitFor(() => expect(screen.getByRole('alert')).toBeInTheDocument())
    expect(screen.getByText('Credit Lens could not be reached. Check the connection and try again.')).toBeInTheDocument()
    expect(screen.queryByText('010190-123A')).not.toBeInTheDocument()
  })

  it('generates a new client ID when the submitted data changes after an error', async () => {
    const user = userEvent.setup()
    const uuidMock = vi.fn()
      .mockReturnValueOnce('first-client-id')
      .mockReturnValueOnce('second-client-id')
    vi.stubGlobal('crypto', { randomUUID: uuidMock })
    const fetchMock = vi.fn().mockResolvedValue(problemResponse(400))
    vi.stubGlobal('fetch', fetchMock)
    render(<App />)
    const identityInput = screen.getByLabelText(/personal identity code/i)
    await user.type(identityInput, '010190-123A')
    await user.click(screen.getByRole('button', { name: /create financing request/i }))
    await screen.findByRole('alert')
    await user.clear(identityInput)
    await user.type(identityInput, '010190-124A')
    await user.click(screen.getByRole('button', { name: /create financing request/i }))

    const payloads = fetchMock.mock.calls.map((call) => JSON.parse(call[1].body as string) as { clientRequestId: string })
    expect(payloads.map((payload) => payload.clientRequestId)).toEqual(['first-client-id', 'second-client-id'])
  })
})
