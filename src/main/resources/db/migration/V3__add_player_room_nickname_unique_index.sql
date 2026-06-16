CREATE UNIQUE INDEX ux_players_room_nickname_lower ON players (room_id, lower(nickname));
