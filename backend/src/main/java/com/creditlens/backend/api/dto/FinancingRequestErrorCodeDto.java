package com.creditlens.backend.api.dto;

public enum FinancingRequestErrorCodeDto {
    PCR_TIMEOUT("The Positive Credit Register did not respond in time."),
    PCR_UNAVAILABLE("The Positive Credit Register is currently unavailable."),
    PCR_REJECTED("The Positive Credit Register rejected the request."),
    PCR_INVALID_RESPONSE("The Positive Credit Register returned an invalid response."),
    PCR_CALL_INTERRUPTED("The Positive Credit Register call was interrupted before completion.");

    private final String defaultMessage;

    FinancingRequestErrorCodeDto(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
