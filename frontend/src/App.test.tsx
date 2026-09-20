import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { cleanup, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import App from "./App";

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

describe("Credit Lens workspace", () => {
  beforeEach(() =>
    vi.stubGlobal("crypto", { randomUUID: vi.fn(() => "generated-client-id") }),
  );
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  it("shows a focused error summary and keeps the invalid field linked", async () => {
    const user = userEvent.setup();
    vi.stubGlobal("fetch", vi.fn());
    render(<App />);
    await user.click(screen.getByRole("button", { name: "Request extract" }));
    expect(screen.getByRole("alert")).toHaveTextContent("There is a problem");
    expect(screen.getByRole("alert")).toHaveTextContent("required format");
    expect(screen.getByLabelText(/personal identity code/i)).toHaveAttribute(
      "aria-describedby",
      expect.stringContaining("error"),
    );
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
    await screen.findByRole("heading", { name: /took too long/i });
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

  it("puts an active ban first in the result", async () => {
    const user = userEvent.setup();
    const body = {
      ...responseBody,
      creditExtractSummary: {
        ...responseBody.creditExtractSummary,
        voluntaryBanOnCredits: { isInEffect: true, reason: "Consumer request" },
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
    expect(alert).toHaveTextContent("Consumer request");
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
    expect(screen.getByText("Active")).toBeInTheDocument();
    expect(
      screen.queryByText("Active voluntary credit ban"),
    ).not.toBeInTheDocument();
    expect(screen.queryByText("010190-123A")).not.toBeInTheDocument();
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
      screen.getByText(/Credit Lens could not be reached/),
    ).toBeInTheDocument();
    expect(screen.queryByText("010190-123A")).not.toBeInTheDocument();
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
    await user.click(input);
    await user.type(input, "010190-124A");
    await user.click(screen.getByRole("button", { name: "Request extract" }));
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
    await screen.findByText("history-extract-0");
    await user.click(screen.getByRole("button", { name: "Next" }));
    await screen.findByRole("alert");
    expect(screen.getByText("history-extract-0")).toBeInTheDocument();
    expect(JSON.parse(fetchMock.mock.calls[1][1].body as string)).toEqual({
      personalIdentityCode: "010190-123A",
      page: 1,
      size: 20,
    });
  });
});
