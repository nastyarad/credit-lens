package com.creditlens.backend;

import static org.assertj.core.api.Assertions.assertThat;

import com.creditlens.backend.support.BackendApiIntegrationTestSupport;
import org.junit.jupiter.api.Test;

class DatabaseMigrationIntegrationTest extends BackendApiIntegrationTestSupport {

  @Test
  void appliesDatabaseMigration() {
    Integer appliedMigrationCount =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM flyway_schema_history
            WHERE version = '1' AND success = TRUE
            """,
            Integer.class);

    assertThat(appliedMigrationCount).isEqualTo(1);
  }
}
