CREATE TABLE rooms (
    id UUID PRIMARY KEY,
    code VARCHAR(8) NOT NULL UNIQUE,
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

CREATE INDEX idx_players_room_id ON players(room_id);
