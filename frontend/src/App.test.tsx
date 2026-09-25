import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { cleanup, render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import App from "./App";
import { FinancingRequestDetailsView } from "./components/FinancingRequestDetails";

const responseBody = {
  id: "request-id",
  clientRequestId: "client-id",
  consumer: { id: "consumer-id", maskedPersonalIdentityCode: "******-123A" },
  creditRegisterExtractPurposes: ["NewConsumerCredit"],
  requestedAt: "2026-09-18T10:15:29Z",
  completedAt: "2026-09-18T10:15:30Z",
  creditExtractSummary: {
    extractReference: "extract-reference",
    creationTimeUtc: "2026-09-18T10:15:30Z",
    voluntaryBanOnCredits: { isInEffect: false, reason: null },
    lendersCount: 0,
    loanContractsCount: 0,
    guaranteedLoanContractsCount: 0,
  },
};
function historyResponse(
  items = [
    {
      id: "history-id",
      clientRequestId: "history-client-id",
      maskedPersonalIdentityCode: "******-123A",
      requestedAt: "2026-09-18T10:15:29Z",
      completedAt: "2026-09-18T10:15:30Z",
      extractReference: "history-extract",
      voluntaryCreditBanActive: true,
    },
  ],
) {
  return new Response(
    JSON.stringify({
      items,
      page: 0,
      size: 20,
      totalItems: items.length,
      totalPages: 1,
    }),
    { status: 200 },
  );
}
function pagedHistoryResponse(page: number, totalPages: number) {
  return new Response(
    JSON.stringify({
      items: [
        {
          id: `history-${page}`,
          clientRequestId: `client-${page}`,
          maskedPersonalIdentityCode: "******-123A",
          requestedAt: "2026-09-18T10:15:29Z",
          completedAt: "2026-09-18T10:15:30Z",
          extractReference: `history-extract-${page}`,
          voluntaryCreditBanActive: false,
        },
      ],
      page,
      size: 20,
      totalItems: totalPages,
      totalPages,
    }),
    { status: 200 },
  );
}
function problemResponse(status: number) {
  return new Response(
    JSON.stringify({
      title: "Register unavailable",
      detail: "Try again later.",
      correlationId: "correlation-reference",
    }),
    { status, headers: { "Content-Type": "application/problem+json" } },
  );
}
function detailsResponse() {
  return new Response(
    JSON.stringify({
      id: "history-id",
      clientRequestId: "history-client-id",
      consumer: {
        id: "consumer-id",
        maskedPersonalIdentityCode: "******-123A",
      },
      creditRegisterExtractPurposes: ["NewConsumerCredit"],
      requestedAt: "2026-09-18T10:15:29Z",
      completedAt: "2026-09-18T10:15:30Z",
      creditExtract: {
        extractReference: "history-extract",
        creationTimeUtc: "2026-09-18T10:15:30Z",
        voluntaryBanOnCredits: { isInEffect: false, reason: null },
        creditInformationSummary: {
          lendersCount: 0,
          loanContractsCount: 0,
          guaranteedLoanContractsCount: 0,
          repaymentsPaidLastAmount: [],
          sumOfMonthlyLeasingInstalments: [],
        },
        loans: [],
        incomeData: [],
      },
    }),
    { status: 200 },
  );
}

describe("Credit Lens workspace", () => {
  beforeEach(() =>
    vi.stubGlobal("crypto", { randomUUID: vi.fn(() => "generated-client-id") }),
  );
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  it("keeps focus on the invalid field and explains how to fix it", async () => {
    const user = userEvent.setup();
    vi.stubGlobal("fetch", vi.fn());
    render(<App />);
    const input = screen.getByLabelText(/personal identity code/i);
    await user.click(screen.getByRole("button", { name: "Request extract" }));
    expect(screen.getByRole("alert")).toHaveTextContent(
      "Enter a Finnish personal identity code in the required format.",
    );
    expect(screen.getByRole("alert")).toHaveTextContent("required format");
    await waitFor(() => expect(input).toHaveFocus());
    expect(input).toHaveValue("");
    expect(input).toHaveAttribute(
      "aria-describedby",
      expect.stringContaining("error"),
    );
  });

  it("keeps the clear form action stable while the input changes", async () => {
    const user = userEvent.setup();
    vi.stubGlobal("fetch", vi.fn());
    render(<App />);
    const clearButton = screen.getByRole("button", { name: "Clear form" });
    const input = screen.getByLabelText(/personal identity code/i);

    expect(clearButton).toBeDisabled();
    await user.type(input, "010190-123A");
    expect(clearButton).toBeEnabled();
    await user.click(clearButton);
    expect(input).toHaveValue("");
    expect(clearButton).toBeDisabled();
  });

  it("clears the full identity code after submission and renders provenance", async () => {
    const user = userEvent.setup();
    const fetchMock = vi
      .fn()
      .mockResolvedValue(
        new Response(JSON.stringify(responseBody), { status: 201 }),
      );
    vi.stubGlobal("fetch", fetchMock);
    render(<App />);
    const input = screen.getByLabelText(/personal identity code/i);
    await user.type(input, "010190-123A");
    await user.click(screen.getByRole("button", { name: "Request extract" }));
    expect(input).toHaveValue("");
    await screen.findByRole("heading", {
      name: "Register extract received",
      level: 1,
    });
    expect(
      screen.getByText("Finnish Positive Credit Register"),
    ).toBeInTheDocument();
    expect(screen.getByText(/\*{6}-123A/)).toBeInTheDocument();
    expect(screen.queryByText("010190-123A")).not.toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/financing-requests",
      expect.objectContaining({
        body: expect.stringContaining("generated-client-id"),
      }),
    );
  });

  it("maps a timeout to safe recovery and retries the same in-memory request", async () => {
    const user = userEvent.setup();
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(problemResponse(504))
      .mockResolvedValueOnce(
        new Response(JSON.stringify(responseBody), { status: 201 }),
      );
    vi.stubGlobal("fetch", fetchMock);
    render(<App />);
    await user.type(
      screen.getByLabelText(/personal identity code/i),
      "010190-123A",
    );
    await user.click(screen.getByRole("button", { name: "Request extract" }));
    await screen.findByRole("heading", { name: "The request timed out" });
    expect(
      screen.getByText(/No successful request was saved/),
    ).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Retry request" }));
    await screen.findByRole("heading", {
      name: "Register extract received",
      level: 1,
    });
    const first = JSON.parse(fetchMock.mock.calls[0][1].body as string);
    const second = JSON.parse(fetchMock.mock.calls[1][1].body as string);
    expect(second.clientRequestId).toBe(first.clientRequestId);
    expect(second.personalIdentityCode).toBe(first.personalIdentityCode);
  });

  it("ignores an abandoned request after navigating away", async () => {
    const user = userEvent.setup();
    let resolveFirstRequest!: (response: Response) => void;
    const firstRequest = new Promise<Response>((resolve) => {
      resolveFirstRequest = resolve;
    });
    const secondResponse = {
      ...responseBody,
      id: "second-request-id",
      consumer: {
        id: "second-consumer-id",
        maskedPersonalIdentityCode: "******-456B",
      },
    };
    const fetchMock = vi
      .fn()
      .mockImplementationOnce(() => firstRequest)
      .mockResolvedValueOnce(
        new Response(JSON.stringify(secondResponse), { status: 201 }),
      );
    vi.stubGlobal("fetch", fetchMock);
    render(<App />);

    await user.type(
      screen.getByLabelText(/personal identity code/i),
      "010190-123A",
    );
    await user.click(screen.getByRole("button", { name: "Request extract" }));
    await user.click(screen.getByRole("button", { name: "Request history" }));
    await user.click(screen.getByRole("button", { name: "New request" }));
    await user.type(
      screen.getByLabelText(/personal identity code/i),
      "020290-456B",
    );
    await user.click(screen.getByRole("button", { name: "Request extract" }));

    expect(await screen.findByText("******-456B")).toBeInTheDocument();
    resolveFirstRequest(
      new Response(JSON.stringify(responseBody), { status: 201 }),
    );
    await waitFor(() => {
      expect(screen.getByText("******-456B")).toBeInTheDocument();
      expect(screen.queryByText("******-123A")).not.toBeInTheDocument();
    });
  });

  it("puts an active ban first in the result", async () => {
    const user = userEvent.setup();
    const body = {
      ...responseBody,
      creditExtractSummary: {
        ...responseBody.creditExtractSummary,
        voluntaryBanOnCredits: {
          isInEffect: true,
          reason: "ControlOfPersonalFinances",
        },
      },
    };
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValue(new Response(JSON.stringify(body), { status: 201 })),
    );
    render(<App />);
    await user.type(
      screen.getByLabelText(/personal identity code/i),
      "010190-123A",
    );
    await user.click(screen.getByRole("button", { name: "Request extract" }));
    const alert = await screen.findByRole("alert");
    expect(alert).toHaveTextContent("Active voluntary credit ban");
    expect(alert).toHaveTextContent("Control of personal finances");
    expect(alert).not.toHaveTextContent("ControlOfPersonalFinances");
    expect(
      screen.queryByText(/approved|declined|eligible/i),
    ).not.toBeInTheDocument();
  });

  it("searches history without retaining the full code and renders a semantic table", async () => {
    const user = userEvent.setup();
    const fetchMock = vi.fn().mockResolvedValue(historyResponse());
    vi.stubGlobal("fetch", fetchMock);
    render(<App />);
    await user.click(screen.getByRole("button", { name: "Request history" }));
    await user.type(
      screen.getByLabelText(/personal identity code/i),
      "010190-123A",
    );
    await user.click(
      screen.getByRole("button", { name: "Search request history" }),
    );
    await screen.findByRole("table");
    expect(screen.getByRole("table")).toHaveAccessibleName(
      "Successful request history",
    );
    expect(
      screen.getByText("Successful requests for ******-123A"),
    ).toBeInTheDocument();
    expect(screen.getByText("Active")).toBeInTheDocument();
    expect(
      screen.queryByText("Active voluntary credit ban"),
    ).not.toBeInTheDocument();
    expect(screen.queryByText("010190-123A")).not.toBeInTheDocument();
  });

  it("returns focus to the invalid history input without an error summary", async () => {
    const user = userEvent.setup();
    vi.stubGlobal("fetch", vi.fn());
    render(<App />);
    await user.click(screen.getByRole("button", { name: "Request history" }));
    const input = screen.getByLabelText(/personal identity code/i);
    await user.click(
      screen.getByRole("button", { name: "Search request history" }),
    );

    expect(screen.getByRole("alert")).toHaveTextContent(
      "Enter a Finnish personal identity code in the required format.",
    );
    await waitFor(() => expect(input).toHaveFocus());
    expect(input).toHaveValue("");
    expect(screen.queryByText("Check the form")).not.toBeInTheDocument();
  });

  it("shows one masked consumer context and only requested, ban, and details columns", async () => {
    const user = userEvent.setup();
    const extractReference = "2fdc1e0b-91d8-4d7c-9d4c-82df9c3b2a10";
    const fetchMock = vi.fn().mockResolvedValue(
      historyResponse([
        {
          id: "history-id",
          clientRequestId: "history-client-id",
          maskedPersonalIdentityCode: "******-123A",
          requestedAt: "2026-09-18T10:15:29Z",
          completedAt: "2026-09-18T10:15:30Z",
          extractReference,
          voluntaryCreditBanActive: true,
        },
      ]),
    );
    vi.stubGlobal("fetch", fetchMock);
    render(<App />);
    await user.click(screen.getByRole("button", { name: "Request history" }));
    await user.type(
      screen.getByLabelText(/personal identity code/i),
      "010190-123A",
    );
    await user.click(
      screen.getByRole("button", { name: "Search request history" }),
    );

    const table = await screen.findByRole("table");
    const [, row] = within(table).getAllByRole("row");
    const cells = within(row).getAllByRole("cell");
    expect(cells).toHaveLength(3);
    const context = screen.getByText("Successful requests for ******-123A");
    expect(context).toHaveTextContent("Successful requests for ******-123A");
    expect(row).not.toHaveTextContent("******-123A");
    expect(
      within(table).queryByRole("columnheader", { name: "Consumer" }),
    ).not.toBeInTheDocument();
    expect(
      within(table).queryByRole("columnheader", {
        name: "Extract reference",
      }),
    ).not.toBeInTheDocument();
    expect(within(row).queryByText(extractReference)).not.toBeInTheDocument();
    expect(cells[2]).toHaveTextContent("View details");
    expect(
      within(cells[2]).getByRole("button", {
        name: `View details for extract ${extractReference}`,
      }),
    ).toBeInTheDocument();
  });

  it("uses one page-level heading on request details", async () => {
    const user = userEvent.setup();
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValueOnce(historyResponse())
        .mockResolvedValueOnce(detailsResponse()),
    );
    render(<App />);

    await user.click(screen.getByRole("button", { name: "Request history" }));
    await user.type(
      screen.getByLabelText(/personal identity code/i),
      "010190-123A",
    );
    await user.click(
      screen.getByRole("button", { name: "Search request history" }),
    );
    await user.click(
      await screen.findByRole("button", {
        name: "View details for extract history-extract",
      }),
    );

    await screen.findByRole("heading", {
      name: "Credit register extract",
      level: 1,
    });
    expect(screen.getAllByRole("heading", { level: 1 })).toHaveLength(1);
  });

  it("returns from request details to the previously loaded history table", async () => {
    const user = userEvent.setup();
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValueOnce(historyResponse())
        .mockResolvedValueOnce(detailsResponse()),
    );
    render(<App />);

    await user.click(screen.getByRole("button", { name: "Request history" }));
    await user.type(
      screen.getByLabelText(/personal identity code/i),
      "010190-123A",
    );
    await user.click(
      screen.getByRole("button", { name: "Search request history" }),
    );
    await user.click(
      await screen.findByRole("button", {
        name: "View details for extract history-extract",
      }),
    );
    await screen.findByRole("heading", {
      name: "Credit register extract",
      level: 1,
    });

    await user.click(
      screen.getByRole("button", { name: /Back to request history/ }),
    );

    expect(
      screen.getByRole("button", {
        name: "View details for extract history-extract",
      }),
    ).toBeInTheDocument();
    expect(screen.getByText("Successful requests for ******-123A")).toBeVisible();
  });

  it("explains an empty history result", async () => {
    const user = userEvent.setup();
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(historyResponse([])));
    render(<App />);
    await user.click(screen.getByRole("button", { name: "Request history" }));
    await user.type(
      screen.getByLabelText(/personal identity code/i),
      "010190-123A",
    );
    await user.click(
      screen.getByRole("button", { name: "Search request history" }),
    );
    await waitFor(() =>
      expect(
        screen.getByText("No successful requests found for this consumer."),
      ).toBeInTheDocument(),
    );
    expect(
      screen.getByText(/request that was rejected or failed/i),
    ).toBeInTheDocument();
  });

  it("shows a safe network error without exposing sensitive input", async () => {
    const user = userEvent.setup();
    vi.stubGlobal(
      "fetch",
      vi.fn().mockRejectedValue(new TypeError("Failed to fetch")),
    );
    render(<App />);
    await user.type(
      screen.getByLabelText(/personal identity code/i),
      "010190-123A",
    );
    await user.click(screen.getByRole("button", { name: "Request extract" }));
    await waitFor(() => expect(screen.getByRole("alert")).toBeInTheDocument());
    expect(
      screen.getByText("The service is temporarily unavailable"),
    ).toBeInTheDocument();
    expect(screen.queryByText("010190-123A")).not.toBeInTheDocument();
  });

  it("uses generic copy for an API error and preserves its support reference", async () => {
    const user = userEvent.setup();
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            title: "Upstream service failure",
            detail: "The register returned internal diagnostics.",
            correlationId: "correlation-reference",
          }),
          { status: 503, headers: { "Content-Type": "application/problem+json" } },
        ),
      ),
    );
    render(<App />);
    await user.type(
      screen.getByLabelText(/personal identity code/i),
      "010190-123A",
    );
    await user.click(screen.getByRole("button", { name: "Request extract" }));

    await screen.findByRole("heading", {
      name: "The service is temporarily unavailable",
    });
    expect(screen.getByText("Try again later. No successful request was saved.")).toBeInTheDocument();
    expect(screen.getByText("correlation-reference")).toBeInTheDocument();
    expect(screen.queryByText("Upstream service failure")).not.toBeInTheDocument();
    expect(screen.queryByText(/internal diagnostics/i)).not.toBeInTheDocument();
  });

  it("uses generic copy when request history cannot be loaded", async () => {
    const user = userEvent.setup();
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            title: "Database timeout",
            detail: "Internal database diagnostics.",
            correlationId: "history-correlation-reference",
          }),
          { status: 500, headers: { "Content-Type": "application/problem+json" } },
        ),
      ),
    );
    render(<App />);
    await user.click(screen.getByRole("button", { name: "Request history" }));
    await user.type(
      screen.getByLabelText(/personal identity code/i),
      "010190-123A",
    );
    await user.click(
      screen.getByRole("button", { name: "Search request history" }),
    );

    await screen.findByRole("heading", {
      name: "We could not load request history",
    });
    expect(
      screen.getByText("Try again. If the problem continues, contact support."),
    ).toBeInTheDocument();
    expect(
      screen.getByText("history-correlation-reference"),
    ).toBeInTheDocument();
    expect(screen.queryByText("Database timeout")).not.toBeInTheDocument();
    expect(screen.queryByText(/internal database diagnostics/i)).not.toBeInTheDocument();
  });

  it("uses generic copy when request details cannot be loaded", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            title: "Unexpected upstream response",
            detail: "Internal response diagnostics.",
            correlationId: "details-correlation-reference",
          }),
          { status: 500, headers: { "Content-Type": "application/problem+json" } },
        ),
      ),
    );
    render(<FinancingRequestDetailsView id="request-id" onBack={vi.fn()} />);

    expect(
      await screen.findByText("We could not load request details"),
    ).toBeInTheDocument();
    expect(
      screen.getByText("Try again. If the problem continues, contact support."),
    ).toBeInTheDocument();
    expect(screen.getByText(/Support reference: details-correlation-reference/)).toBeInTheDocument();
    expect(screen.queryByText("Unexpected upstream response")).not.toBeInTheDocument();
    expect(screen.queryByText(/internal response diagnostics/i)).not.toBeInTheDocument();
  });

  it("shows income amounts in EUR without the technical currency warning", async () => {
    const user = userEvent.setup();
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            id: "request-id",
            clientRequestId: "client-id",
            consumer: { id: "consumer-id", maskedPersonalIdentityCode: "******-123A" },
            creditRegisterExtractPurposes: ["NewConsumerCredit"],
            requestedAt: "2026-09-18T10:15:29Z",
            completedAt: "2026-09-18T10:15:30Z",
            creditExtract: {
              extractReference: "extract-reference",
              creationTimeUtc: "2026-09-18T10:15:30Z",
              voluntaryBanOnCredits: { isInEffect: false, reason: null },
              creditInformationSummary: {
                lendersCount: 0,
                loanContractsCount: 0,
                guaranteedLoanContractsCount: 0,
                repaymentsPaidLastAmount: [],
                sumOfMonthlyLeasingInstalments: [],
              },
              loans: [],
              incomeData: [
                {
                  year: 2026,
                  months: [
                    {
                      month: 1,
                      wagesGrossAmount: 4000,
                      wagesNetAmount: 3200,
                      benefitsGrossAmount: 500,
                      benefitsNetAmount: 400,
                    },
                  ],
                },
              ],
            },
          }),
          { status: 200 },
        ),
      ),
    );
    render(<FinancingRequestDetailsView id="request-id" onBack={vi.fn()} />);
    await screen.findByRole("heading", { name: "Credit register extract" });
    await user.click(screen.getByText("Income (1 months)"));

    expect(screen.getByText("Income amounts (EUR)")).toBeInTheDocument();
    expect(screen.getByText("EUR 4,000")).toBeInTheDocument();
    expect(screen.getByText("EUR 3,200")).toBeInTheDocument();
    expect(screen.getByText("EUR 500")).toBeInTheDocument();
    expect(screen.getByText("EUR 400")).toBeInTheDocument();
    expect(screen.queryByText(/currency not provided/i)).not.toBeInTheDocument();
  });

  it("generates a new client ID when a failed submission changes", async () => {
    const user = userEvent.setup();
    const uuidMock = vi
      .fn()
      .mockReturnValueOnce("first-client-id")
      .mockReturnValueOnce("second-client-id");
    vi.stubGlobal("crypto", { randomUUID: uuidMock });
    const fetchMock = vi.fn().mockResolvedValue(problemResponse(400));
    vi.stubGlobal("fetch", fetchMock);
    render(<App />);
    const input = screen.getByLabelText(/personal identity code/i);
    await user.type(input, "010190-123A");
    await user.click(screen.getByRole("button", { name: "Request extract" }));
    await screen.findByRole("alert");
    await waitFor(() =>
      expect(
        screen.getByRole("heading", {
          name: "Request a credit register extract",
          level: 1,
        }),
      ).toHaveFocus(),
    );
    await user.click(input);
    await user.type(input, "010190-124A");
    await user.click(screen.getByRole("button", { name: "Request extract" }));
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2));
    const payloads = fetchMock.mock.calls.map(
      (call) =>
        JSON.parse(call[1].body as string) as { clientRequestId: string },
    );
    expect(payloads.map((payload) => payload.clientRequestId)).toEqual([
      "first-client-id",
      "second-client-id",
    ]);
  });

  it("does not search when opening request history", async () => {
    const user = userEvent.setup();
    const fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);
    render(<App />);
    await user.click(screen.getByRole("button", { name: "Request history" }));
    expect(
      screen.getByRole("heading", { name: "Request history", level: 1 }),
    ).toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("keeps the previous history page visible when pagination fails", async () => {
    const user = userEvent.setup();
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(pagedHistoryResponse(0, 2))
      .mockResolvedValueOnce(problemResponse(503));
    vi.stubGlobal("fetch", fetchMock);
    render(<App />);
    await user.click(screen.getByRole("button", { name: "Request history" }));
    await user.type(
      screen.getByLabelText(/personal identity code/i),
      "010190-123A",
    );
    await user.click(
      screen.getByRole("button", { name: "Search request history" }),
    );
    await screen.findByRole("button", {
      name: "View details for extract history-extract-0",
    });
    await user.click(screen.getByRole("button", { name: "Next" }));
    await screen.findByRole("alert");
    expect(
      screen.getByRole("button", {
        name: "View details for extract history-extract-0",
      }),
    ).toBeInTheDocument();
    expect(JSON.parse(fetchMock.mock.calls[1][1].body as string)).toEqual({
      personalIdentityCode: "010190-123A",
      page: 1,
      size: 20,
    });
  });
});
