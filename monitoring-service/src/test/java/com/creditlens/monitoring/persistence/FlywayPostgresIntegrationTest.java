package com.creditlens.monitoring.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class FlywayPostgresIntegrationTest {
  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

  @Test
  void appliesMonitoringReportSchemaToPostgres() throws Exception {
    Flyway flyway =
        Flyway.configure()
            .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
            .load();

    assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
    try (var connection = POSTGRES.createConnection("")) {
      assertThat(connection.getMetaData().getTables(null, null, "monitoring_report", null).next())
          .isTrue();
      assertThat(
              connection.getMetaData().getTables(null, null, "monitoring_report_item", null).next())
          .isTrue();
    }
  }
}
