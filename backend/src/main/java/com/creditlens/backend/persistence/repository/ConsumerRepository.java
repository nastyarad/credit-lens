package com.creditlens.backend.persistence.repository;

import com.creditlens.backend.persistence.entity.ConsumerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ConsumerRepository extends JpaRepository<ConsumerEntity, UUID> {

    Optional<ConsumerEntity> findByPersonalIdentityCode(String personalIdentityCode);
}
