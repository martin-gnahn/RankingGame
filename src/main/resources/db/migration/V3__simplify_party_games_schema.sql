DROP TABLE IF EXISTS ranking_entries;
DROP TABLE IF EXISTS answers;
DROP TABLE IF EXISTS rounds;
DROP TABLE IF EXISTS scores;
DROP TABLE IF EXISTS game_sessions;
DROP TABLE IF EXISTS questions;

ALTER TABLE rooms
    DROP CONSTRAINT IF EXISTS fk_rooms_host_player;

ALTER TABLE rooms
    DROP COLUMN IF EXISTS host_player_id;
