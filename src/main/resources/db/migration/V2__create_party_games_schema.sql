CREATE TABLE rooms (
    id UUID PRIMARY KEY,
    code VARCHAR(8) NOT NULL UNIQUE,
    host_player_id UUID,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE players (
    id UUID PRIMARY KEY,
    room_id UUID NOT NULL REFERENCES rooms(id),
    nickname VARCHAR(80) NOT NULL,
    is_host BOOLEAN NOT NULL,
    connection_status VARCHAR(32) NOT NULL,
    joined_at TIMESTAMPTZ NOT NULL
);

ALTER TABLE rooms
    ADD CONSTRAINT fk_rooms_host_player
        FOREIGN KEY (host_player_id) REFERENCES players(id);

CREATE TABLE questions (
    id UUID PRIMARY KEY,
    text VARCHAR(500) NOT NULL,
    category VARCHAR(80),
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE game_sessions (
    id UUID PRIMARY KEY,
    room_id UUID NOT NULL REFERENCES rooms(id),
    game_type VARCHAR(50) NOT NULL,
    status VARCHAR(32) NOT NULL,
    current_round_number INTEGER NOT NULL
);

CREATE TABLE rounds (
    id UUID PRIMARY KEY,
    game_session_id UUID NOT NULL REFERENCES game_sessions(id),
    question_id UUID NOT NULL REFERENCES questions(id),
    round_number INTEGER NOT NULL,
    state VARCHAR(32) NOT NULL
);

CREATE TABLE answers (
    id UUID PRIMARY KEY,
    round_id UUID NOT NULL REFERENCES rounds(id),
    player_id UUID NOT NULL REFERENCES players(id),
    text VARCHAR(500) NOT NULL,
    submitted_at TIMESTAMPTZ NOT NULL,
    UNIQUE (round_id, player_id)
);

CREATE TABLE ranking_entries (
    id UUID PRIMARY KEY,
    round_id UUID NOT NULL REFERENCES rounds(id),
    answer_id UUID NOT NULL REFERENCES answers(id),
    position INTEGER NOT NULL,
    UNIQUE (round_id, position),
    UNIQUE (round_id, answer_id)
);

CREATE TABLE scores (
    id UUID PRIMARY KEY,
    game_session_id UUID NOT NULL REFERENCES game_sessions(id),
    player_id UUID NOT NULL REFERENCES players(id),
    points INTEGER NOT NULL,
    UNIQUE (game_session_id, player_id)
);

CREATE INDEX idx_players_room_id ON players(room_id);
CREATE INDEX idx_game_sessions_room_id ON game_sessions(room_id);
CREATE INDEX idx_rounds_game_session_id ON rounds(game_session_id);
CREATE INDEX idx_answers_round_id ON answers(round_id);
CREATE INDEX idx_ranking_entries_round_id ON ranking_entries(round_id);
CREATE INDEX idx_scores_game_session_id ON scores(game_session_id);
