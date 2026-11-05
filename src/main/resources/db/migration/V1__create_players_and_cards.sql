CREATE TABLE players (
    id   BIGINT PRIMARY KEY CHECK (id > 0),
    name TEXT   NOT NULL CHECK (btrim(name) <> ''),
    cash BIGINT NOT NULL DEFAULT 0 CHECK (cash >= 0)
);

-- Общий пул карточек (каталог). Наполняется из JSON-конфигов при старте бота.
CREATE TABLE card_templates (
    id         TEXT    PRIMARY KEY,
    name       TEXT    NOT NULL CHECK (btrim(name) <> ''),
    universe   TEXT    NOT NULL CHECK (btrim(universe) <> ''),
    rarity     TEXT    NOT NULL CHECK (rarity IN
                   ('COMMON', 'UNCOMMON', 'RARE', 'EPIC', 'MYTHIC', 'LEGENDARY', 'DIVINE')),
    base_hp    INT     NOT NULL CHECK (base_hp >= 0),
    base_st    INT     NOT NULL CHECK (base_st >= 0),
    base_df    INT     NOT NULL CHECK (base_df >= 0),
    is_starter BOOLEAN NOT NULL DEFAULT FALSE
);

-- Карточки игрока: ссылка на шаблон из общего пула.
-- Максимум одна копия каждой карточки (составной ключ), повтор превращается в осколки.
-- Итоговые характеристики не хранятся, а считаются: база шаблона x редкость + усиления.
CREATE TABLE player_cards (
    player_id   BIGINT      NOT NULL REFERENCES players (id) ON DELETE CASCADE,
    template_id TEXT        NOT NULL REFERENCES card_templates (id),
    hp_boost    INT         NOT NULL DEFAULT 0 CHECK (hp_boost >= 0),  -- сколько раз усилено HP
    st_boost    INT         NOT NULL DEFAULT 0 CHECK (st_boost >= 0),  -- сколько раз усилено ST
    df_boost    INT         NOT NULL DEFAULT 0 CHECK (df_boost >= 0),  -- сколько раз усилено DF
    shards      INT         NOT NULL DEFAULT 0 CHECK (shards >= 0),    -- осколки этой карточки
    obtained_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (player_id, template_id)
);
