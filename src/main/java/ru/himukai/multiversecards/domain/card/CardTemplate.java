
package ru.himukai.multiversecards.domain.card;

import java.util.Objects;

/**
 * Шаблон карточки из общего каталога.
 *
 * @param starter входит ли карточка в стартовый набор
 */
public record CardTemplate(
        TemplateId id,
        String name,
        String universe,
        Rarity rarity,
        Stats baseStats,
        boolean starter
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

    /**
     * Совместимость со старым конструктором.
     * По умолчанию карточка не входит в стартовый набор.
     */
    public CardTemplate(
            TemplateId id,
            String name,
            String universe,
            Rarity rarity,
            Stats baseStats
    ) {
        this(id, name, universe, rarity, baseStats, false);
    }

    public String imageResource() {
        return "cards/images/" + id.value() + ".png";
    }
}
