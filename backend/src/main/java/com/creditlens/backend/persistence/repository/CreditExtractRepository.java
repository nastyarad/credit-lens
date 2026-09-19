package com.creditlens.backend.persistence.repository;

import com.creditlens.backend.persistence.entity.CreditExtractEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CreditExtractRepository extends JpaRepository<CreditExtractEntity, UUID> {

    Optional<CreditExtractEntity> findByFinancingRequest_Id(UUID financingRequestId);
}
