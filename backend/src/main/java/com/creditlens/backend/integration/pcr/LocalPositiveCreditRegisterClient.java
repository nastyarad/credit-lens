package com.creditlens.backend.integration.pcr;

import com.creditlens.backend.domain.CreditExtract;
import com.creditlens.backend.domain.CreditInformationSummary;
import com.creditlens.backend.domain.CreditRegisterExtractPurpose;
import com.creditlens.backend.domain.PersonalIdentityCode;
import com.creditlens.backend.domain.VoluntaryBanOnCredits;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class LocalPositiveCreditRegisterClient implements PositiveCreditRegisterClient {

    private final Clock clock;

    public LocalPositiveCreditRegisterClient(Clock clock) {
        this.clock = clock;
    }

    @Override
    public CreditExtract requestCreditExtract(
            PersonalIdentityCode personalIdentityCode,
            List<CreditRegisterExtractPurpose> purposes
    ) {
        Instant creationTime = clock.instant();
        return new CreditExtract(
                UUID.randomUUID(),
                UUID.randomUUID(),
                creationTime,
                new VoluntaryBanOnCredits(false, null),
                new CreditInformationSummary(0, 0, 0),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                null
        );
    }
}
