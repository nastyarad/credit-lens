package com.creditlens.monitoring.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "monitoring_report_item")
public class MonitoringReportItemEntity {
  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "report_id", nullable = false)
  private MonitoringReportEntity report;

  @Column(nullable = false)
  private int ordinal;

  @Column(name = "masked_personal_identity_code", nullable = false)
  private String maskedPersonalIdentityCode;

  @Column(name = "requested_at", nullable = false)
  private Instant requestedAt;

  @Column(name = "completed_at", nullable = false)
  private Instant completedAt;

  @Column(name = "voluntary_credit_ban_reason", nullable = false)
  private String voluntaryCreditBanReason;

  @Column(name = "lenders_count", nullable = false)
  private int lendersCount;

  @Column(name = "loan_contracts_count", nullable = false)
  private int loanContractsCount;

  @Column(name = "guaranteed_loan_contracts_count", nullable = false)
  private int guaranteedLoanContractsCount;

  protected MonitoringReportItemEntity() {}

  public MonitoringReportItemEntity(
      int ordinal,
      String maskedPersonalIdentityCode,
      Instant requestedAt,
      Instant completedAt,
      String voluntaryCreditBanReason,
      int lendersCount,
      int loanContractsCount,
      int guaranteedLoanContractsCount) {
    this.id = UUID.randomUUID();
    this.ordinal = ordinal;
    this.maskedPersonalIdentityCode = maskedPersonalIdentityCode;
    this.requestedAt = requestedAt;
    this.completedAt = completedAt;
    this.voluntaryCreditBanReason = voluntaryCreditBanReason;
    this.lendersCount = lendersCount;
    this.loanContractsCount = loanContractsCount;
    this.guaranteedLoanContractsCount = guaranteedLoanContractsCount;
  }

  void setReport(MonitoringReportEntity report) {
    this.report = report;
  }

  public int getOrdinal() {
    return ordinal;
  }

  public String getMaskedPersonalIdentityCode() {
    return maskedPersonalIdentityCode;
  }

  public Instant getRequestedAt() {
    return requestedAt;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public String getVoluntaryCreditBanReason() {
    return voluntaryCreditBanReason;
  }

  public int getLendersCount() {
    return lendersCount;
  }

  public int getLoanContractsCount() {
    return loanContractsCount;
  }

  public int getGuaranteedLoanContractsCount() {
    return guaranteedLoanContractsCount;
  }
}
