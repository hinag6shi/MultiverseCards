ALTER TABLE players
    ADD COLUMN starter_set_claimed BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE players p
SET starter_set_claimed = TRUE
WHERE EXISTS (
    SELECT 1
    FROM player_cards pc
    JOIN card_templates ct ON ct.id = pc.template_id
    WHERE pc.player_id = p.id AND ct.is_starter = TRUE
);
