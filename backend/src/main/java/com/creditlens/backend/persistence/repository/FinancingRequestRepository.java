package com.creditlens.backend.persistence.repository;

import com.creditlens.backend.persistence.entity.FinancingRequestEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FinancingRequestRepository extends JpaRepository<FinancingRequestEntity, UUID> {

  @EntityGraph(attributePaths = "consumer")
  Optional<FinancingRequestEntity> findByClientRequestId(UUID clientRequestId);

  // Keep this query next to the repository method while it is small, static, used once, and
  // covered by integration tests: the SQL and its projection contract remain easy to discover.
  // In production, prefer a custom repository with JdbcClient or jOOQ when queries require dynamic
  // composition or type-safe reuse, and external named SQL when large queries need independent
  // ownership, tuning, or versioning.
  @Query(
      value =
          """
          SELECT f.id AS id,
                 f.client_request_id AS clientRequestId,
                 c.personal_identity_code AS personalIdentityCode,
                 f.requested_at AS requestedAt,
                 f.completed_at AS completedAt,
                 e.extract_reference AS extractReference,
                 e.voluntary_ban_active AS voluntaryCreditBanActive
            FROM financing_request f
            JOIN consumer c ON c.id = f.consumer_id
            JOIN credit_extract e ON e.financing_request_id = f.id
           WHERE c.personal_identity_code = :personalIdentityCode
           ORDER BY f.requested_at DESC, f.id DESC
          """,
      countQuery =
          """
          SELECT COUNT(*)
            FROM financing_request f
            JOIN consumer c ON c.id = f.consumer_id
            JOIN credit_extract e ON e.financing_request_id = f.id
           WHERE c.personal_identity_code = :personalIdentityCode
          """,
      nativeQuery = true)
  Page<FinancingRequestHistoryProjection> findHistoryByPersonalIdentityCode(
      String personalIdentityCode, Pageable pageable);

  @Override
  @EntityGraph(attributePaths = "consumer")
  Optional<FinancingRequestEntity> findById(UUID id);
}
