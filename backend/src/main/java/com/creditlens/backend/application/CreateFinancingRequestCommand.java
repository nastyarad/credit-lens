package com.creditlens.backend.application;

import com.creditlens.backend.domain.CreditRegisterExtractPurpose;
import com.creditlens.backend.domain.PersonalIdentityCode;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record CreateFinancingRequestCommand(
        UUID clientRequestId,
        PersonalIdentityCode personalIdentityCode,
        List<CreditRegisterExtractPurpose> purposes
) {

    public CreateFinancingRequestCommand {
        Objects.requireNonNull(clientRequestId, "clientRequestId must not be null");
        Objects.requireNonNull(personalIdentityCode, "personalIdentityCode must not be null");
        purposes = List.copyOf(purposes);
        if (purposes.isEmpty()) {
            throw new IllegalArgumentException("at least one extract purpose is required");
        }
    }
}
