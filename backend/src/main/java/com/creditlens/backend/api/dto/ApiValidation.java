package com.creditlens.backend.api.dto;

final class ApiValidation {

    static final String PERSONAL_IDENTITY_CODE_PATTERN =
            "^[0-9]{6}[+\\-A-FYXWVU][0-9]{3}[0-9A-FHJ-NPR-Y]$";

    private ApiValidation() {
    }
}
