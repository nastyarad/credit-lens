package com.creditlens.backend.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Consumer(UUID id, PersonalIdentityCode personalIdentityCode, Instant createdAt) {

  public Consumer {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(personalIdentityCode, "personalIdentityCode must not be null");
    Objects.requireNonNull(createdAt, "createdAt must not be null");
  }
}
