CREATE TABLE round_card_assignments (
    id UUID PRIMARY KEY,
    round_id UUID NOT NULL REFERENCES rounds(id),
    player_id UUID NOT NULL REFERENCES players(id),
    card_value INTEGER NOT NULL,
    UNIQUE (round_id, player_id),
    UNIQUE (round_id, card_value),
    CONSTRAINT chk_round_card_assignments_card_value CHECK (card_value BETWEEN 1 AND 10)
);

CREATE INDEX idx_round_card_assignments_round_id ON round_card_assignments(round_id);
