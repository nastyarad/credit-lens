package com.creditlens.monitoring.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "monitoring_report")
public class MonitoringReportEntity {
  @Id private UUID id;

  @Column(name = "interval_start", nullable = false)
  private Instant intervalStart;

  @Column(name = "interval_end", nullable = false)
  private Instant intervalEnd;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private MonitoringReportStatus status;

  @Column(nullable = false)
  private String recipient;

  @Column(nullable = false)
  private String subject;

  @Column(nullable = false, columnDefinition = "text")
  private String body;

  @Column(name = "content_type", nullable = false)
  private String contentType;

  @Column(name = "item_count", nullable = false)
  private int itemCount;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "sent_at")
  private Instant sentAt;

  @Column(name = "last_attempt_at")
  private Instant lastAttemptAt;

  @Column(name = "attempt_count", nullable = false)
  private int attemptCount;

  @Column(name = "last_error")
  private String lastError;

  @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("ordinal ASC")
  private List<MonitoringReportItemEntity> items = new ArrayList<>();

  protected MonitoringReportEntity() {}

  public MonitoringReportEntity(
      UUID id,
      Instant intervalStart,
      Instant intervalEnd,
      String recipient,
      String subject,
      String body,
      Instant createdAt,
      List<MonitoringReportItemEntity> items) {
    this.id = id;
    this.intervalStart = intervalStart;
    this.intervalEnd = intervalEnd;
    this.status = MonitoringReportStatus.CREATED;
    this.recipient = recipient;
    this.subject = subject;
    this.body = body;
    this.contentType = "text/plain; charset=UTF-8";
    this.itemCount = items.size();
    this.createdAt = createdAt;
    for (MonitoringReportItemEntity item : items) addItem(item);
  }

  public UUID getId() {
    return id;
  }

  public Instant getIntervalStart() {
    return intervalStart;
  }

  public Instant getIntervalEnd() {
    return intervalEnd;
  }

  public MonitoringReportStatus getStatus() {
    return status;
  }

  public String getRecipient() {
    return recipient;
  }

  public String getSubject() {
    return subject;
  }

  public String getBody() {
    return body;
  }

  public int getItemCount() {
    return itemCount;
  }

  public int getAttemptCount() {
    return attemptCount;
  }

  public String getLastError() {
    return lastError;
  }

  public List<MonitoringReportItemEntity> getItems() {
    return List.copyOf(items);
  }

  public void markSent(Instant attemptedAt) {
    if (status != MonitoringReportStatus.CREATED && status != MonitoringReportStatus.FAILED) {
      throw new IllegalStateException("only unsent reports can be marked sent");
    }
    status = MonitoringReportStatus.SENT;
    sentAt = attemptedAt;
    lastAttemptAt = attemptedAt;
    attemptCount++;
    lastError = null;
  }

  public void markFailed(Instant attemptedAt, String safeError) {
    if (status != MonitoringReportStatus.CREATED && status != MonitoringReportStatus.FAILED) {
      throw new IllegalStateException("only unsent reports can be marked failed");
    }
    status = MonitoringReportStatus.FAILED;
    lastAttemptAt = attemptedAt;
    attemptCount++;
    lastError = safeError;
  }

  private void addItem(MonitoringReportItemEntity item) {
    item.setReport(this);
    items.add(item);
  }
}
