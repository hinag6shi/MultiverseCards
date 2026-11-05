package ru.himukai.multiversecards.domain.card;

import java.util.Objects;

/** Карточка из общего пула. У игрока есть только ссылка на неё (см. {@code OwnedCard}). */
public record CardTemplate(
        TemplateId id,
        String name,
        String universe,
        Rarity rarity,
        Stats baseStats
) {
    public CardTemplate {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(rarity, "rarity");
        Objects.requireNonNull(baseStats, "baseStats");

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name не может быть пустым");
        }

        if (universe == null || universe.isBlank()) {
            throw new IllegalArgumentException("universe не может быть пустым");
        }
    }
}
