package com.creditlens.monitoring.report;

import com.creditlens.monitoring.integration.backend.MonitoringFinancingRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class MonitoringReportRenderer {

  public RenderedReport render(
      String recipient,
      Instant intervalStart,
      Instant intervalEnd,
      List<MonitoringFinancingRequest> items) {
    String subject =
        "Credit Lens voluntary credit ban report " + intervalStart + " to " + intervalEnd;
    StringBuilder body = new StringBuilder();
    body.append("Credit Lens voluntary credit ban monitoring report\n\n");
    body.append("UTC interval: [")
        .append(intervalStart)
        .append(", ")
        .append(intervalEnd)
        .append(")\n");
    body.append("Total records: ").append(items.size()).append("\n\n");
    if (items.isEmpty()) {
      body.append(
          "No completed financing requests with an active voluntary credit ban were found.\n");
    } else {
      for (int index = 0; index < items.size(); index++)
        appendItem(body, index + 1, items.get(index));
    }
    return new RenderedReport(recipient, subject, body.toString());
  }

  private static void appendItem(
      StringBuilder body, int position, MonitoringFinancingRequest item) {
    body.append("Record ").append(position).append(":\n");
    body.append("Masked identity code: ").append(item.maskedPersonalIdentityCode()).append("\n");
    body.append("Requested at: ").append(item.requestedAt()).append("\n");
    body.append("Completed at: ").append(item.completedAt()).append("\n");
    body.append("Voluntary credit ban reason: ")
        .append(item.voluntaryCreditBanReason())
        .append("\n");
    body.append("Lenders count: ").append(item.lendersCount()).append("\n");
    body.append("Loan contracts count: ").append(item.loanContractsCount()).append("\n");
    body.append("Guaranteed loan contracts count: ")
        .append(item.guaranteedLoanContractsCount())
        .append("\n\n");
  }
}
