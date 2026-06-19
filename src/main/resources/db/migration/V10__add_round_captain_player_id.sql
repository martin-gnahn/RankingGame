ALTER TABLE rounds
    ADD COLUMN captain_player_id UUID;

UPDATE rounds
SET captain_player_id = rooms.host_player_id
FROM game_sessions
JOIN rooms ON rooms.id = game_sessions.room_id
WHERE rounds.game_session_id = game_sessions.id
  AND rounds.captain_player_id IS NULL
  AND rooms.host_player_id IS NOT NULL;

ALTER TABLE rounds
    ALTER COLUMN captain_player_id SET NOT NULL;

ALTER TABLE rounds
    ADD CONSTRAINT fk_rounds_captain_player
        FOREIGN KEY (captain_player_id) REFERENCES players(id);

CREATE INDEX idx_rounds_captain_player_id ON rounds(captain_player_id);
