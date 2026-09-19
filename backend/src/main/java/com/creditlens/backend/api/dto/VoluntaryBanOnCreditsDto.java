package com.creditlens.backend.api.dto;

public record VoluntaryBanOnCreditsDto(boolean isInEffect, VoluntaryCreditBanReasonDto reason) {

  public VoluntaryBanOnCreditsDto {
    if (isInEffect && reason == null) {
      throw new IllegalArgumentException("an active voluntary ban requires a reason");
    }
    if (!isInEffect && reason != null) {
      throw new IllegalArgumentException("an inactive voluntary ban cannot have a reason");
    }
  }
}
