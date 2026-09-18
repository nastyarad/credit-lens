package com.creditlens.backend.api.dto;

import java.math.BigDecimal;
import java.util.Objects;

public record CurrencyAmountDto(String currencyCode, BigDecimal sum) {

    public CurrencyAmountDto {
        Objects.requireNonNull(currencyCode, "currencyCode must not be null");
        Objects.requireNonNull(sum, "sum must not be null");
    }
}
