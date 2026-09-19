package com.creditlens.backend.application;

import com.creditlens.backend.domain.CreditExtract;
import com.creditlens.backend.domain.FinancingRequest;
import com.creditlens.backend.persistence.entity.CreditExtractEntity;
import com.creditlens.backend.persistence.entity.FinancingRequestEntity;
import com.creditlens.backend.persistence.repository.CreditExtractRepository;
import com.creditlens.backend.persistence.repository.FinancingRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class CompleteFinancingRequestTransaction {

    private final FinancingRequestRepository financingRequestRepository;
    private final CreditExtractRepository creditExtractRepository;
    private final Clock clock;

    public CompleteFinancingRequestTransaction(
            FinancingRequestRepository financingRequestRepository,
            CreditExtractRepository creditExtractRepository,
            Clock clock
    ) {
        this.financingRequestRepository = financingRequestRepository;
        this.creditExtractRepository = creditExtractRepository;
        this.clock = clock;
    }

    @Transactional
    public FinancingRequest execute(UUID financingRequestId, CreditExtract registerExtract) {
        FinancingRequestEntity requestEntity = financingRequestRepository.findById(financingRequestId)
                .orElseThrow(() -> new IllegalStateException("financing request disappeared before completion"));

        Instant completedAt = clock.instant();
        CreditExtract persistedExtract = registerExtract.withPersistenceMetadata(
                registerExtract.id(),
                completedAt
        );
        requestEntity.complete(persistedExtract, completedAt);
        creditExtractRepository.save(new CreditExtractEntity(persistedExtract, requestEntity));
        financingRequestRepository.save(requestEntity);

        return requestEntity.toDomain(persistedExtract);
    }
}
