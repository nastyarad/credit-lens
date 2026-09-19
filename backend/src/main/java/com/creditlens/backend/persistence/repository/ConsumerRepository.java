package com.creditlens.backend.persistence.repository;

import com.creditlens.backend.persistence.entity.ConsumerEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsumerRepository extends JpaRepository<ConsumerEntity, UUID> {

  Optional<ConsumerEntity> findByPersonalIdentityCode(String personalIdentityCode);
}
