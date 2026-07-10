ALTER TABLE players
    ADD COLUMN session_token_hash VARsesCHAR(64),
    ADD COLUMN session_expires_at TIMESTAMP;