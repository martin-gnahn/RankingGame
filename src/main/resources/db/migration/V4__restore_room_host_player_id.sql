ALTER TABLE rooms
    ADD COLUMN IF NOT EXISTS host_player_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_rooms_host_player'
    ) THEN
        ALTER TABLE rooms
            ADD CONSTRAINT fk_rooms_host_player
                FOREIGN KEY (host_player_id) REFERENCES players(id);
    END IF;
END $$;
