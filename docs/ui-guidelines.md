# Credit Lens UI guidelines

This document defines the current interaction and presentation rules for the
implemented React frontend. It is intended for developers and reviewers
checking product clarity, privacy, accessibility, and responsive behaviour.

## Design principles

1. **Show register facts, not decisions.** Never label financing approved,
   declined, safe, or high risk.
2. **Put critical facts first.** Ban status and source provenance precede
   detailed financial data.
3. **Remove sensitive input after use.** The full identity code is transient UI
   state; results use the masked API value.
4. **Keep one clear task per view.** New request, history, and extract details
   have distinct headings and actions.
5. **Explain recovery.** Errors state whether a successful request was saved
   and offer a safe next action.
6. **Use progressive disclosure.** Summary and provenance stay visible;
   detailed extract sections can expand as needed.

## Active voluntary ban

An active ban is the first result region after the page heading. Present an
icon, the text **Active voluntary credit ban**, the register reason when
available, and a statement that Credit Lens does not make a financing decision.

Use a high-contrast alert surface and `role="alert"`. Colour supports the
message but is never the only signal. Do not provide a dismiss or resolve
action, and do not invent expiry, consent, or policy data.

When no active ban is reported, use equally clear neutral wording. Avoid
describing the consumer or request as safe.

## Privacy

- Accept the full personal identity code only in the create and history-search
  fields.
- Clear it after a valid submission and keep retry data only in memory for the
  current interaction.
- Never put the full value in a URL, page title, DOM identifier, breadcrumb,
  result, error, log, analytics event, accessible-only copy, or screenshot.
- Render only safe problem fields from the API. Never display a raw response
  body or upstream payload.
- Use synthetic identity codes in demos and tests.

## Validation and error recovery

Validate the identity code on submit. Preserve correct non-sensitive choices,
associate inline error text with the input, and move focus to the invalid field
or an error summary.

Differentiate these recoverable states with safe copy:

| State | User guidance |
| --- | --- |
| PCR rejection (`422`) | Review the input and start a new request if needed |
| Timeout (`504`) | Retry the same submission |
| Upstream unavailable or invalid (`502`) | Retry later |
| Client request conflict (`409`) | Start a new request |
| Browser/network failure | Check connectivity and retry |

Technical retry reuses the in-memory `clientRequestId`. Starting a new request
generates a different ID. Do not auto-retry or imply exactly-once PCR execution.

History and detail failures keep their surrounding navigation available. A
detail `404` returns the user to history; transient load failures offer retry.

## Focus management

- Provide a skip link to the main content.
- Move focus to the view heading after navigation or successful submission.
- Move focus to the relevant validation field or error panel after failure.
- Use `role="status"` with a polite live region while waiting.
- Do not announce the personal identity code.
- Keep visible keyboard focus and a logical tab order; avoid focus traps.

## Responsive history and details

Use a semantic table with a caption for comparable history or income rows on
wide screens. On narrow screens, preserve every label and value in a readable
card or key/value layout. Critical ban and provenance information must remain
above collapsible details and must not require horizontal scrolling.

Detail sections follow this order: provenance, summary, repayments and leasing,
loans, delayed amounts, and income. Use definition lists for metadata and
tables only for repeated comparable values. Preserve currency and date context.

## Accessibility acceptance checks

- One `<h1>` per view and a logical heading hierarchy.
- Native buttons, links, inputs, selects, tables, captions, and definition
  lists where they match the interaction.
- Text contrast of at least 4.5:1 and non-text indicators of at least 3:1.
- Content reflows at 320 CSS pixels and remains usable at 200% zoom.
- Touch targets meet at least 24 by 24 CSS pixels; 44 pixels is preferred.
- Motion respects `prefers-reduced-motion`.
- Ban status, validation, loading, success, retry, pagination, empty history,
  and detail loading are tested by keyboard and a screen reader.
- Automated accessibility checks supplement, but do not replace, manual tests.

## Unresolved product assumptions

- The exact lender role, language, device mix, and frequency of use need product
  validation.
- The legally approved action and wording after an active ban are not defined
  by this PoC.
- Locale, timezone, and financial number-format conventions need agreement.
- Whether client request IDs or extract references should be copyable support
  identifiers needs a privacy and operations decision.
- Authentication, authorization, audit visibility, and retention workflows are
  outside the current UI scope.

Superseded design exploration and wireframes are retained under
[`archive/`](archive/) for decision history. They are not current product
documentation.
