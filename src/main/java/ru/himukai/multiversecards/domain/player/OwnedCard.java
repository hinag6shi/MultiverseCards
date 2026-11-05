package ru.himukai.multiversecards.domain.player;

import ru.himukai.multiversecards.domain.card.Boosts;
import ru.himukai.multiversecards.domain.card.CardTemplate;
import ru.himukai.multiversecards.domain.card.TemplateId;

import java.time.Instant;
import java.util.Objects;

/**
 * Карточка в коллекции игрока: шаблон из общего пула + то, что есть только у этого игрока.
 *
 * @param shards осколки этой карточки (даёт повтор, тратятся на её усиления)
 */
public record OwnedCard(CardTemplate template, Boosts boosts, int shards, Instant obtainedAt) {

    public OwnedCard {
        Objects.requireNonNull(template, "template");
        Objects.requireNonNull(boosts, "boosts");
        Objects.requireNonNull(obtainedAt, "obtainedAt");

        if (shards < 0) {
            throw new IllegalArgumentException("shards не может быть отрицательным");
        }
    }

    /** Только что полученная карточка: без усилений и осколков. */
    public static OwnedCard fresh(CardTemplate template, Instant obtainedAt) {
        return new OwnedCard(template, Boosts.NONE, 0, obtainedAt);
    }

    public TemplateId templateId() {
        return template.id();
    }

    public OwnedCard withBoosts(Boosts newBoosts) {
        return new OwnedCard(template, newBoosts, shards, obtainedAt);
    }

    public OwnedCard withShards(int newShards) {
        return new OwnedCard(template, boosts, newShards, obtainedAt);
    }
}
