package com.creditlens.backend.application;

import com.creditlens.backend.domain.Consumer;
import com.creditlens.backend.domain.CreditExtract;
import com.creditlens.backend.domain.FinancingRequest;
import com.creditlens.backend.persistence.entity.ConsumerEntity;
import com.creditlens.backend.persistence.entity.FinancingRequestEntity;
import com.creditlens.backend.persistence.repository.ConsumerRepository;
import com.creditlens.backend.persistence.repository.CreditExtractRepository;
import com.creditlens.backend.persistence.repository.FinancingRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class StartFinancingRequestTransaction {

    private final ConsumerRepository consumerRepository;
    private final FinancingRequestRepository financingRequestRepository;
    private final CreditExtractRepository creditExtractRepository;
    private final Clock clock;

    public StartFinancingRequestTransaction(
            ConsumerRepository consumerRepository,
            FinancingRequestRepository financingRequestRepository,
            CreditExtractRepository creditExtractRepository,
            Clock clock
    ) {
        this.consumerRepository = consumerRepository;
        this.financingRequestRepository = financingRequestRepository;
        this.creditExtractRepository = creditExtractRepository;
        this.clock = clock;
    }

    @Transactional
    public CreateFinancingRequestResult execute(CreateFinancingRequestCommand command) {
        var existing = financingRequestRepository.findByClientRequestId(command.clientRequestId());
        if (existing.isPresent()) {
            FinancingRequest request = toDomain(existing.orElseThrow());
            if (!request.hasSameInput(command.personalIdentityCode(), command.purposes())) {
                throw new ClientRequestConflictException();
            }
            return new CreateFinancingRequestResult(request, false);
        }

        Instant now = clock.instant();
        ConsumerEntity consumerEntity = consumerRepository
                .findByPersonalIdentityCode(command.personalIdentityCode().value())
                .orElseGet(() -> consumerRepository.save(new ConsumerEntity(new Consumer(
                        UUID.randomUUID(),
                        command.personalIdentityCode(),
                        now
                ))));

        FinancingRequest request = FinancingRequest.start(
                UUID.randomUUID(),
                command.clientRequestId(),
                consumerEntity.toDomain(),
                command.purposes(),
                now
        );
        financingRequestRepository.save(new FinancingRequestEntity(request, consumerEntity));
        return new CreateFinancingRequestResult(request, true);
    }

    private FinancingRequest toDomain(FinancingRequestEntity entity) {
        CreditExtract extract = creditExtractRepository.findByFinancingRequest_Id(entity.getId())
                .map(creditExtract -> creditExtract.toDomain())
                .orElse(null);
        return entity.toDomain(extract);
    }
}
