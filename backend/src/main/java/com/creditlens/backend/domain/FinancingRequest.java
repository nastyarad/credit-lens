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
    private FinancingRequestStatus status;
    private Instant completedAt;
    private String errorCode;
    private String errorMessage;
    private CreditExtract creditExtract;

    private FinancingRequest(
            UUID id,
            UUID clientRequestId,
            Consumer consumer,
            List<CreditRegisterExtractPurpose> extractPurposes,
            FinancingRequestStatus status,
            Instant requestedAt,
            Instant completedAt,
            String errorCode,
            String errorMessage,
            CreditExtract creditExtract
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.clientRequestId = Objects.requireNonNull(clientRequestId, "clientRequestId must not be null");
        this.consumer = Objects.requireNonNull(consumer, "consumer must not be null");
        this.extractPurposes = List.copyOf(extractPurposes);
        if (this.extractPurposes.isEmpty()) {
            throw new IllegalArgumentException("at least one extract purpose is required");
        }
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.requestedAt = Objects.requireNonNull(requestedAt, "requestedAt must not be null");
        this.completedAt = completedAt;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.creditExtract = creditExtract;
        validateState();
    }

    public static FinancingRequest start(
            UUID id,
            UUID clientRequestId,
            Consumer consumer,
            List<CreditRegisterExtractPurpose> extractPurposes,
            Instant requestedAt
    ) {
        return new FinancingRequest(
                id,
                clientRequestId,
                consumer,
                extractPurposes,
                FinancingRequestStatus.IN_PROGRESS,
                requestedAt,
                null,
                null,
                null,
                null
        );
    }

    public static FinancingRequest restore(
            UUID id,
            UUID clientRequestId,
            Consumer consumer,
            List<CreditRegisterExtractPurpose> extractPurposes,
            FinancingRequestStatus status,
            Instant requestedAt,
            Instant completedAt,
            String errorCode,
            String errorMessage,
            CreditExtract creditExtract
    ) {
        return new FinancingRequest(
                id,
                clientRequestId,
                consumer,
                extractPurposes,
                status,
                requestedAt,
                completedAt,
                errorCode,
                errorMessage,
                creditExtract
        );
    }

    public void complete(CreditExtract extract, Instant completionTime) {
        if (status != FinancingRequestStatus.IN_PROGRESS) {
            throw new IllegalStateException("only an in-progress financing request can be completed");
        }
        if (completionTime.isBefore(requestedAt)) {
            throw new IllegalArgumentException("completion time must not be before request time");
        }
        creditExtract = Objects.requireNonNull(extract, "extract must not be null");
        completedAt = completionTime;
        status = FinancingRequestStatus.COMPLETED;
        validateState();
    }

    public boolean hasSameInput(PersonalIdentityCode personalIdentityCode,
                                List<CreditRegisterExtractPurpose> purposes) {
        return consumer.personalIdentityCode().equals(personalIdentityCode)
                && extractPurposes.equals(purposes);
    }

    private void validateState() {
        if (completedAt != null && completedAt.isBefore(requestedAt)) {
            throw new IllegalArgumentException("completion time must not be before request time");
        }
        switch (status) {
            case IN_PROGRESS -> {
                if (completedAt != null || errorCode != null || errorMessage != null || creditExtract != null) {
                    throw new IllegalArgumentException("invalid in-progress financing request state");
                }
            }
            case COMPLETED -> {
                if (completedAt == null || errorCode != null || errorMessage != null || creditExtract == null) {
                    throw new IllegalArgumentException("invalid completed financing request state");
                }
            }
            case FAILED -> {
                if (completedAt == null || errorCode == null || creditExtract != null) {
                    throw new IllegalArgumentException("invalid failed financing request state");
                }
            }
        }
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

    public FinancingRequestStatus status() {
        return status;
    }

    public Instant requestedAt() {
        return requestedAt;
    }

    public Instant completedAt() {
        return completedAt;
    }

    public String errorCode() {
        return errorCode;
    }

    public String errorMessage() {
        return errorMessage;
    }

    public CreditExtract creditExtract() {
        return creditExtract;
    }
}
