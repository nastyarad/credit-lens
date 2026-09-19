package com.creditlens.backend.api.dto;

import java.util.Objects;
import java.util.UUID;

public record ConsumerDto(UUID id, String maskedPersonalIdentityCode) {

    public ConsumerDto {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(maskedPersonalIdentityCode, "maskedPersonalIdentityCode must not be null");
    }
}
