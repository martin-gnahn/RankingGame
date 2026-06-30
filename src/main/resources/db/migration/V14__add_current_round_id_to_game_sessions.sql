ALTER TABLE game_sessions
    ADD COLUMN current_round_id UUID;

UPDATE game_sessions
SET current_round_id = current_round.id FROM (
    SELECT DISTINCT ON (game_session_id)
        id,
        game_session_id
    FROM rounds
    ORDER BY
        game_session_id,
        CASE state
            WHEN 'ANSWER_SUBMISSION' THEN 0
            ELSE 1
        END,
        id
) current_round
WHERE current_round.game_session_id = game_sessions.id
  AND game_sessions.current_round_id IS NULL;

ALTER TABLE game_sessions
    ALTER COLUMN current_round_id SET NOT NULL;

ALTER TABLE game_sessions
    ADD CONSTRAINT fk_game_sessions_current_round
        FOREIGN KEY (current_round_id)
            REFERENCES rounds (id)
            DEFERRABLE INITIALLY DEFERRED;

CREATE INDEX idx_game_sessions_current_round_id ON game_sessions (current_round_id);

DROP INDEX IF EXISTS idx_rounds_game_session_id;
