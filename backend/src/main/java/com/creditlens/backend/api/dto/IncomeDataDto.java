package com.creditlens.backend.api.dto;

import java.util.List;

public record IncomeDataDto(int year, List<MonthlyIncomeDto> months) {
  public IncomeDataDto {
    months = List.copyOf(months);
  }
}
