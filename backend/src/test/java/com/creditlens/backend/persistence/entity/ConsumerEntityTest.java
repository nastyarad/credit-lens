package com.creditlens.backend.persistence.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.creditlens.backend.domain.Consumer;
import com.creditlens.backend.domain.PersonalIdentityCode;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConsumerEntityTest {
  @Test
  void mapsConsumerToEntityAndBack() {
    UUID id = UUID.fromString("33333333-3333-3333-3333-333333333333");
    Instant createdAt = Instant.parse("2026-09-19T10:15:30Z");
    Consumer consumer = new Consumer(id, PersonalIdentityCode.of("010190-123A"), createdAt);

    ConsumerEntity entity = new ConsumerEntity(consumer);

    assertThat(entity.getId()).isEqualTo(id);
    assertThat(entity.getPersonalIdentityCode()).isEqualTo("010190-123A");
    assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
    assertThat(entity.toDomain()).isEqualTo(consumer);
  }
}
