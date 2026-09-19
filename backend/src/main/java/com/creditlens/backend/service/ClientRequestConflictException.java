package com.creditlens.backend.service;

public class ClientRequestConflictException extends RuntimeException {

  public ClientRequestConflictException() {
    super("clientRequestId was already used with different input");
  }
}
