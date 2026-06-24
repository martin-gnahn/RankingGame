CREATE TABLE chat_messages (
    id UUID PRIMARY KEY,
    room_id UUID NOT NULL REFERENCES rooms(id),
    player_id UUID NOT NULL REFERENCES players(id),
    sender_nickname VARCHAR(80) NOT NULL,
    body VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_chat_messages_room_created_at ON chat_messages(room_id, created_at);
CREATE INDEX idx_chat_messages_player_id ON chat_messages(player_id);
