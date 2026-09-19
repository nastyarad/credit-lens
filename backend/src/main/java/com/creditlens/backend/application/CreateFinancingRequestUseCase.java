package com.creditlens.backend.application;

import com.creditlens.backend.domain.CreditExtract;
import com.creditlens.backend.domain.FinancingRequest;
import com.creditlens.backend.integration.pcr.PositiveCreditRegisterClient;
import org.springframework.stereotype.Service;

@Service
public class CreateFinancingRequestUseCase {

    private final StartFinancingRequestTransaction startTransaction;
    private final CompleteFinancingRequestTransaction completeTransaction;
    private final PositiveCreditRegisterClient positiveCreditRegisterClient;

    public CreateFinancingRequestUseCase(
            StartFinancingRequestTransaction startTransaction,
            CompleteFinancingRequestTransaction completeTransaction,
            PositiveCreditRegisterClient positiveCreditRegisterClient
    ) {
        this.startTransaction = startTransaction;
        this.completeTransaction = completeTransaction;
        this.positiveCreditRegisterClient = positiveCreditRegisterClient;
    }

    public CreateFinancingRequestResult execute(CreateFinancingRequestCommand command) {
        CreateFinancingRequestResult started = startTransaction.execute(command);
        if (!started.created()) {
            return started;
        }

        FinancingRequest request = started.financingRequest();
        CreditExtract extract = positiveCreditRegisterClient.requestCreditExtract(
                request.consumer().personalIdentityCode(),
                request.extractPurposes()
        );
        FinancingRequest completed = completeTransaction.execute(request.id(), extract);
        return new CreateFinancingRequestResult(completed, true);
    }
}
