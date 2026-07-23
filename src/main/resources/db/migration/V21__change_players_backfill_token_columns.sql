create or replace function get_token_hash(id uuid) returns text as
$$
begin
    return md5('legacy-token-a:' || id::text) || md5('legacy-token-b:' || id::text);
end
$$ language plpgsql;

UPDATE players
SET session_token_hash = get_token_hash(id)
WHERE session_token_hash IS NULL
   OR session_token_hash = 'OLD_TOKEN'
   OR session_token_hash LIKE 'room_for_%';

UPDATE players
SET session_expires_at = NOW()
WHERE session_expires_at IS NULL
   OR session_token_hash = 'OLD_TOKEN'
   OR session_token_hash LIKE 'room_for_%';

drop function if exists get_token_hash(room_id uuid, id uuid);