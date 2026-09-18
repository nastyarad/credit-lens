package com.creditlens.backend.api.dto;

import java.util.Objects;

public record FinancingRequestErrorDto(FinancingRequestErrorCodeDto code, String message) {

    public FinancingRequestErrorDto {
        Objects.requireNonNull(code, "code must not be null");
        message = message == null || message.isBlank() ? code.defaultMessage() : message;
    }
}
