ALTER TABLE players
    ADD COLUMN session_token_hash VARCHAR(64),
    ADD COLUMN session_expires_at TIMESTAMP;