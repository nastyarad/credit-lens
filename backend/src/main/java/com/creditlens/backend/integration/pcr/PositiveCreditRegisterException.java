package com.creditlens.backend.integration.pcr;

public class PositiveCreditRegisterException extends RuntimeException {
  public enum Kind {
    TIMEOUT,
    UNAVAILABLE,
    REJECTED,
    INVALID_RESPONSE
  }

  private final Kind kind;

  public PositiveCreditRegisterException(Kind kind) {
    super(kind.name());
    this.kind = kind;
  }

  public Kind kind() {
    return kind;
  }
}
