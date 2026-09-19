package com.creditlens.backend.persistence.entity;

import com.creditlens.backend.domain.CreditExtract;
import com.creditlens.backend.domain.CreditRegisterExtractPurpose;
import com.creditlens.backend.domain.FinancingRequest;
import com.creditlens.backend.domain.FinancingRequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "financing_request")
public class FinancingRequestEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consumer_id", nullable = false)
    private ConsumerEntity consumer;

    @Column(name = "client_request_id", nullable = false)
    private UUID clientRequestId;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "extract_purposes", nullable = false, columnDefinition = "varchar[]")
    private String[] extractPurposes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private FinancingRequestStatus status;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "error_code", length = 64)
    private String errorCode;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    protected FinancingRequestEntity() {
    }

    public FinancingRequestEntity(FinancingRequest request, ConsumerEntity consumer) {
        id = request.id();
        this.consumer = consumer;
        clientRequestId = request.clientRequestId();
        extractPurposes = request.extractPurposes().stream().map(Enum::name).toArray(String[]::new);
        status = request.status();
        requestedAt = request.requestedAt();
        completedAt = request.completedAt();
        errorCode = request.errorCode();
        errorMessage = request.errorMessage();
    }

    public FinancingRequest toDomain(CreditExtract creditExtract) {
        List<CreditRegisterExtractPurpose> purposes = Arrays.stream(extractPurposes)
                .map(CreditRegisterExtractPurpose::valueOf)
                .toList();
        return FinancingRequest.restore(
                id,
                clientRequestId,
                consumer.toDomain(),
                purposes,
                status,
                requestedAt,
                completedAt,
                errorCode,
                errorMessage,
                creditExtract
        );
    }

    public void complete(CreditExtract extract, Instant completionTime) {
        FinancingRequest request = toDomain(null);
        request.complete(extract, completionTime);
        status = request.status();
        completedAt = request.completedAt();
        errorCode = request.errorCode();
        errorMessage = request.errorMessage();
    }

    public UUID getId() {
        return id;
    }

    public ConsumerEntity getConsumer() {
        return consumer;
    }

    public UUID getClientRequestId() {
        return clientRequestId;
    }

    public String[] getExtractPurposes() {
        return extractPurposes.clone();
    }

    public FinancingRequestStatus getStatus() {
        return status;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
