package com.creditlens.backend.api.dto;

import java.net.URI;
import java.util.Objects;
import java.util.UUID;

public record ApiProblemResponse(
    URI type, String title, int status, String detail, URI instance, UUID correlationId) {

  public ApiProblemResponse {
    Objects.requireNonNull(type, "type must not be null");
    Objects.requireNonNull(title, "title must not be null");
    Objects.requireNonNull(detail, "detail must not be null");
    Objects.requireNonNull(instance, "instance must not be null");
    Objects.requireNonNull(correlationId, "correlationId must not be null");
    if (status < 400 || status > 599) {
      throw new IllegalArgumentException("status must be an HTTP error status");
    }
  }
}
