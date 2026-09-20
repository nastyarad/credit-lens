package com.creditlens.backend.service;

import java.util.UUID;

public class FinancingRequestNotFoundException extends RuntimeException {
  public FinancingRequestNotFoundException(UUID id) {
    super("The requested financing request was not found.");
  }
}
