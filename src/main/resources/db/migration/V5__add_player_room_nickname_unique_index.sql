CREATE UNIQUE INDEX IF NOT EXISTS ux_players_room_nickname_lower ON players (room_id, lower(nickname));
