package com.creditlens.backend.persistence.entity;

import com.creditlens.backend.domain.Consumer;
import com.creditlens.backend.domain.PersonalIdentityCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "consumer")
public class ConsumerEntity {

    @Id
    private UUID id;

    @Column(name = "personal_identity_code", nullable = false, length = 11)
    private String personalIdentityCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ConsumerEntity() {
    }

    public ConsumerEntity(Consumer consumer) {
        id = consumer.id();
        personalIdentityCode = consumer.personalIdentityCode().value();
        createdAt = consumer.createdAt();
    }

    public Consumer toDomain() {
        return new Consumer(id, PersonalIdentityCode.of(personalIdentityCode), createdAt);
    }

    public UUID getId() {
        return id;
    }

    public String getPersonalIdentityCode() {
        return personalIdentityCode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
