package com.creditlens.backend.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class FinancingRequest {

  private final UUID id;
  private final UUID clientRequestId;
  private final Consumer consumer;
  private final List<CreditRegisterExtractPurpose> extractPurposes;
  private final Instant requestedAt;
  private final Instant completedAt;
  private final CreditExtract creditExtract;

  private FinancingRequest(
      UUID id,
      UUID clientRequestId,
      Consumer consumer,
      List<CreditRegisterExtractPurpose> extractPurposes,
      Instant requestedAt,
      Instant completedAt,
      CreditExtract creditExtract) {
    this.id = Objects.requireNonNull(id, "id must not be null");
    this.clientRequestId =
        Objects.requireNonNull(clientRequestId, "clientRequestId must not be null");
    this.consumer = Objects.requireNonNull(consumer, "consumer must not be null");
    this.extractPurposes = List.copyOf(extractPurposes);
    if (this.extractPurposes.isEmpty()) {
      throw new IllegalArgumentException("at least one extract purpose is required");
    }
    this.requestedAt = Objects.requireNonNull(requestedAt, "requestedAt must not be null");
    this.completedAt = Objects.requireNonNull(completedAt, "completedAt must not be null");
    if (completedAt.isBefore(requestedAt)) {
      throw new IllegalArgumentException("completion time must not be before request time");
    }
    this.creditExtract = Objects.requireNonNull(creditExtract, "creditExtract must not be null");
  }

  public static FinancingRequest create(
      UUID id,
      UUID clientRequestId,
      Consumer consumer,
      List<CreditRegisterExtractPurpose> extractPurposes,
      Instant requestedAt,
      Instant completedAt,
      CreditExtract creditExtract) {
    return new FinancingRequest(
        id, clientRequestId, consumer, extractPurposes, requestedAt, completedAt, creditExtract);
  }

  public static FinancingRequest restore(
      UUID id,
      UUID clientRequestId,
      Consumer consumer,
      List<CreditRegisterExtractPurpose> extractPurposes,
      Instant requestedAt,
      Instant completedAt,
      CreditExtract creditExtract) {
    return new FinancingRequest(
        id, clientRequestId, consumer, extractPurposes, requestedAt, completedAt, creditExtract);
  }

  public boolean hasSameInput(
      PersonalIdentityCode personalIdentityCode, List<CreditRegisterExtractPurpose> purposes) {
    return consumer.personalIdentityCode().equals(personalIdentityCode)
        && extractPurposes.equals(purposes);
  }

  public UUID id() {
    return id;
  }

  public UUID clientRequestId() {
    return clientRequestId;
  }

  public Consumer consumer() {
    return consumer;
  }

  public List<CreditRegisterExtractPurpose> extractPurposes() {
    return extractPurposes;
  }

  public Instant requestedAt() {
    return requestedAt;
  }

  public Instant completedAt() {
    return completedAt;
  }

  public CreditExtract creditExtract() {
    return creditExtract;
  }
}
