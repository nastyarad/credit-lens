package com.creditlens.backend.api.dto;

import java.time.Instant;

final class ApiValidation {

    static final String PERSONAL_IDENTITY_CODE_PATTERN =
            "^[0-9]{6}[+\\-A-FYXWVU][0-9]{3}[0-9A-FHJ-NPR-Y]$";

    private ApiValidation() {
    }

    static void requireValidFinancingRequestState(
            FinancingRequestStatusDto status,
            Instant requestedAt,
            Instant completedAt,
            FinancingRequestErrorDto error,
            Object creditExtract
    ) {
        if (completedAt != null && completedAt.isBefore(requestedAt)) {
            throw new IllegalArgumentException("completedAt must not be before requestedAt");
        }

        switch (status) {
            case IN_PROGRESS -> {
                if (completedAt != null || error != null || creditExtract != null) {
                    throw new IllegalArgumentException(
                            "an in-progress request cannot have completion data, an error, or a credit extract"
                    );
                }
            }
            case COMPLETED -> {
                if (completedAt == null || error != null || creditExtract == null) {
                    throw new IllegalArgumentException(
                            "a completed request requires completion data and a credit extract, but no error"
                    );
                }
            }
            case FAILED -> {
                if (completedAt == null || error == null || creditExtract != null) {
                    throw new IllegalArgumentException(
                            "a failed request requires completion data and an error, but no credit extract"
                    );
                }
            }
        }
    }
}
