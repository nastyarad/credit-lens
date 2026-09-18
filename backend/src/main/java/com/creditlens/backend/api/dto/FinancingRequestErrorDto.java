package com.creditlens.backend.api.dto;

import java.util.Objects;

public record FinancingRequestErrorDto(FinancingRequestErrorCodeDto code, String message) {

    public FinancingRequestErrorDto {
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(message, "message must not be null");
    }
}
