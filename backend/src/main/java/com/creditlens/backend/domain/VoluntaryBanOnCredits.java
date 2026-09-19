package com.creditlens.backend.domain;

public record VoluntaryBanOnCredits(boolean isInEffect, VoluntaryCreditBanReason reason) {

    public VoluntaryBanOnCredits {
        if (isInEffect && reason == null) {
            throw new IllegalArgumentException("an active voluntary ban requires a reason");
        }
        if (!isInEffect && reason != null) {
            throw new IllegalArgumentException("an inactive voluntary ban cannot have a reason");
        }
    }
}
