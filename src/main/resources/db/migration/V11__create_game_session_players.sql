CREATE TABLE game_session_players (
    game_session_id UUID NOT NULL REFERENCES game_sessions(id),
    player_id UUID NOT NULL REFERENCES players(id),
    PRIMARY KEY (game_session_id, player_id)
);

INSERT INTO game_session_players (game_session_id, player_id)
SELECT game_sessions.id, players.id
FROM game_sessions
JOIN players ON players.room_id = game_sessions.room_id
ON CONFLICT DO NOTHING;

CREATE INDEX idx_game_session_players_player_id ON game_session_players(player_id);
