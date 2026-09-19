package com.creditlens.backend.application;

import com.creditlens.backend.domain.FinancingRequest;

public record CreateFinancingRequestResult(FinancingRequest financingRequest, boolean created) {
}
