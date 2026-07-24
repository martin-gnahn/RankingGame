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

    @Test
    void gameSessionsPointToCurrentRound() {
        Number columnCount = jdbcTemplate.queryForObject("""
                SELECT count(*)
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'game_sessions'
                  AND column_name = 'current_round_id'
                  AND is_nullable = 'NO'
                """, Number.class);

        Number currentRoundIndexCount = jdbcTemplate.queryForObject("""
                SELECT count(*)
                FROM pg_indexes
                WHERE schemaname = 'public'
                  AND indexname = 'idx_game_sessions_current_round_id'
                """, Number.class);

        Number roundGameSessionIndexCount = jdbcTemplate.queryForObject("""
                SELECT count(*)
                FROM pg_indexes
                WHERE schemaname = 'public'
                  AND indexname = 'idx_rounds_game_session_id'
                """, Number.class);

        assertThat(columnCount).isNotNull();
        assertThat(columnCount.intValue()).isOne();
        assertThat(currentRoundIndexCount).isNotNull();
        assertThat(currentRoundIndexCount.intValue()).isOne();
        assertThat(roundGameSessionIndexCount).isNotNull();
        assertThat(roundGameSessionIndexCount.intValue()).isZero();
    }

    @Test
    void gameSessionsStoreCurrentRoundIndex() {
        Number indexColumnCount = jdbcTemplate.queryForObject("""
                SELECT count(*)
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'game_sessions'
                  AND column_name = 'current_round_index'
                  AND data_type = 'integer'
                  AND is_nullable = 'NO'
                """, Number.class);

        Number numberColumnCount = jdbcTemplate.queryForObject("""
                SELECT count(*)
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'game_sessions'
                  AND column_name = 'current_round_number'
                """, Number.class);

        assertThat(indexColumnCount).isNotNull();
        assertThat(indexColumnCount.intValue()).isOne();
        assertThat(numberColumnCount).isNotNull();
        assertThat(numberColumnCount.intValue()).isZero();
    }

    @Test
    void roundsHaveCreatedAtTimestamp() {
        Number columnCount = jdbcTemplate.queryForObject("""
                SELECT count(*)
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'rounds'
                  AND column_name = 'created_at'
                  AND data_type = 'timestamp without time zone'
                  AND is_nullable = 'NO'
                """, Number.class);

        assertThat(columnCount).isNotNull();
        assertThat(columnCount.intValue()).isOne();
    }

    @Test
    void playersHaveSessionTokenInvariants() {
        Number tokenHashColumnCount = jdbcTemplate.queryForObject("""
                SELECT count(*)
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'players'
                  AND column_name = 'session_token_hash'
                  AND is_nullable = 'NO'
                """, Number.class);

        Number sessionExpiryColumnCount = jdbcTemplate.queryForObject("""
                SELECT count(*)
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'players'
                  AND column_name = 'session_expires_at'
                  AND is_nullable = 'NO'
                """, Number.class);

        Number tokenHashIndexCount = jdbcTemplate.queryForObject("""
                SELECT count(*)
                FROM pg_indexes
                WHERE schemaname = 'public'
                  AND indexname = 'ux_players_room_session_token_hash'
                """, Number.class);

        Number helperFunctionCount = jdbcTemplate.queryForObject("""
                SELECT count(*)
                FROM pg_proc
                WHERE pronamespace = 'public'::regnamespace
                  AND proname = 'get_token_hash'
                """, Number.class);

        assertThat(tokenHashColumnCount).isNotNull();
        assertThat(tokenHashColumnCount.intValue()).isOne();
        assertThat(sessionExpiryColumnCount).isNotNull();
        assertThat(sessionExpiryColumnCount.intValue()).isOne();
        assertThat(tokenHashIndexCount).isNotNull();
        assertThat(tokenHashIndexCount.intValue()).isOne();
        assertThat(helperFunctionCount).isNotNull();
        assertThat(helperFunctionCount.intValue()).isZero();
    }
}
