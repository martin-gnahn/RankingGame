package com.example.rankinggame.integration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayMigrationIntegrationTest extends BackendIntegrationTest {
    @Test
    void productsTableIsRemovedByMigrations() {
        Number tableCount = jdbcTemplate.queryForObject("""
                SELECT count(*)
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name = 'products'
                """, Number.class);

        assertThat(tableCount).isNotNull();
        assertThat(tableCount.intValue()).isZero();
    }

    @Test
    void questionsAreSeededByMigrations() {
        Number questionCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM questions WHERE active = TRUE",
                Number.class
        );

        assertThat(questionCount).isNotNull();
        assertThat(questionCount.intValue()).isEqualTo(50);
    }
}
