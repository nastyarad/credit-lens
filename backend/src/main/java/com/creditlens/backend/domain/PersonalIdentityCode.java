package com.creditlens.backend.domain;

import java.util.Objects;
import java.util.regex.Pattern;

public final class PersonalIdentityCode {

  private static final Pattern VALID_FORMAT =
      Pattern.compile("^[0-9]{6}[+\\-A-FYXWVU][0-9]{3}[0-9A-FHJ-NPR-Y]$");

  private final String value;

  private PersonalIdentityCode(String value) {
    this.value = value;
  }

  public static PersonalIdentityCode of(String value) {
    if (value == null || !VALID_FORMAT.matcher(value).matches()) {
      throw new IllegalArgumentException("personal identity code has an invalid format");
    }
    return new PersonalIdentityCode(value);
  }

  public String value() {
    return value;
  }

  public String masked() {
    return "******" + value.substring(6);
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof PersonalIdentityCode that && value.equals(that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    return "<redacted>";
  }
}
