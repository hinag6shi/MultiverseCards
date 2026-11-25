package ru.himukai.multiversecards.domain.player;

import ru.himukai.multiversecards.domain.card.Boosts;
import ru.himukai.multiversecards.domain.card.CardTemplate;
import ru.himukai.multiversecards.domain.card.TemplateId;

import java.time.Instant;
import java.util.Objects;

/**
 * Карточка игрока: общий шаблон плюс его усиления и осколки
 *
 * @param shards сколько осколков накопилось у карточки — их тратим на апгрейды
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

    /** Свежая карточка, пока без усилений и осколков */
    public static OwnedCard fresh(CardTemplate template, Instant obtainedAt) {
        return new OwnedCard(template, Boosts.NONE, 0, obtainedAt);
    }

    public TemplateId templateId() {
        return template.id();
    }

    /** Общий уровень карточки — просто сумма усилений HP, ST и DF */
    public int upgradeLevel() {
        return Math.addExact(Math.addExact(boosts.hp(), boosts.st()), boosts.df());
    }

    public String displayName() {
        return upgradeLevel() == 0 ? template.name() : template.name() + " +" + upgradeLevel();
    }

    public OwnedCard withBoosts(Boosts newBoosts) {
        return new OwnedCard(template, newBoosts, shards, obtainedAt);
    }

    public OwnedCard withShards(int newShards) {
        return new OwnedCard(template, boosts, newShards, obtainedAt);
    }
}
