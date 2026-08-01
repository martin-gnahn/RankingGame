ALTER TABLE players
    ADD COLUMN IF NOT EXISTS session_token_hash VARCHAR(64),
    ADD COLUMN IF NOT EXISTS session_expires_at TIMESTAMP;