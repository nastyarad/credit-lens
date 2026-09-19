package com.creditlens.backend.persistence.entity;

import com.creditlens.backend.domain.CreditExtract;
import com.creditlens.backend.domain.CreditRegisterExtractPurpose;
import com.creditlens.backend.domain.FinancingRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    protected FinancingRequestEntity() {
    }

    public FinancingRequestEntity(FinancingRequest request, ConsumerEntity consumer) {
        id = request.id();
        this.consumer = consumer;
        clientRequestId = request.clientRequestId();
        extractPurposes = request.extractPurposes().stream().map(Enum::name).toArray(String[]::new);
        requestedAt = request.requestedAt();
        completedAt = request.completedAt();
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
                requestedAt,
                completedAt,
                creditExtract
        );
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

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

}
