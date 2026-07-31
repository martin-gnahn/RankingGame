-- Step 1: Add NOT VALID CHECK constraint (instant)
ALTER TABLE players
    ADD CONSTRAINT token_hash_not_null
        CHECK (session_token_hash IS NOT NULL) NOT VALID;

-- Step 2: Validate (scans table, but under safe lock)
ALTER TABLE players VALIDATE CONSTRAINT token_hash_not_null;

-- Step 3: Convert to real NOT NULL (instant — no scan needed!)
-- PG 12+ skips the scan because it sees the validated CHECK
ALTER TABLE players
    ALTER COLUMN session_token_hash SET NOT NULL;

-- Step 4: Drop the now-redundant CHECK constraint
ALTER TABLE players DROP CONSTRAINT token_hash_not_null;

-- Step 1: Add NOT VALID CHECK constraint (instant)
ALTER TABLE players
    ADD CONSTRAINT session_expires_not_null
        CHECK (session_expires_at IS NOT NULL) NOT VALID;

-- Step 2: Validate (scans table, but under safe lock)
ALTER TABLE players VALIDATE CONSTRAINT session_expires_not_null;

-- Step 3: Convert to real NOT NULL (instant — no scan needed!)
-- PG 12+ skips the scan because it sees the validated CHECK
ALTER TABLE players
    ALTER COLUMN session_expires_at SET NOT NULL;

-- Step 4: Drop the now-redundant CHECK constraint
ALTER TABLE players DROP CONSTRAINT session_expires_not_null;

CREATE UNIQUE INDEX IF NOT EXISTS ux_players_room_session_token_hash
    ON players (room_id, session_token_hash);
