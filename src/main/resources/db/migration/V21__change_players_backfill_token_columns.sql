UPDATE players
SET session_token_hash = 'OLD_TOKEN'
WHERE session_token_hash IS NULL;

UPDATE players
SET session_expires_at = NOW()
WHERE session_expires_at IS NULL;