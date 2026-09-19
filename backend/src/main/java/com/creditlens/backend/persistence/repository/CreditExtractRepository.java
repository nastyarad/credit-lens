package com.creditlens.backend.persistence.repository;

import com.creditlens.backend.persistence.entity.CreditExtractEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditExtractRepository extends JpaRepository<CreditExtractEntity, UUID> {

  Optional<CreditExtractEntity> findByFinancingRequest_Id(UUID financingRequestId);
}
