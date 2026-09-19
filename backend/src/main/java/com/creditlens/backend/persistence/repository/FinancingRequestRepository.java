package com.creditlens.backend.persistence.repository;

import com.creditlens.backend.persistence.entity.FinancingRequestEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancingRequestRepository extends JpaRepository<FinancingRequestEntity, UUID> {

  @EntityGraph(attributePaths = "consumer")
  Optional<FinancingRequestEntity> findByClientRequestId(UUID clientRequestId);

  @Override
  @EntityGraph(attributePaths = "consumer")
  Optional<FinancingRequestEntity> findById(UUID id);
}
