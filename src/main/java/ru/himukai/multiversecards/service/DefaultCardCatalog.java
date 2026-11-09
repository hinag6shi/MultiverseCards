
package ru.himukai.multiversecards.service;

import ru.himukai.multiversecards.domain.card.CardTemplate;
import ru.himukai.multiversecards.domain.card.TemplateId;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class DefaultCardCatalog implements CardCatalog {

    private final Map<TemplateId, CardTemplate> cards;
    private final List<CardTemplate> allCards;
    private final List<CardTemplate> starterCards;

    public DefaultCardCatalog(Collection<CardTemplate> templates) {
        Objects.requireNonNull(templates, "templates");

        Map<TemplateId, CardTemplate> indexed = new LinkedHashMap<>();

        for (CardTemplate card : templates) {
            Objects.requireNonNull(card, "card");

            CardTemplate previous = indexed.putIfAbsent(card.id(), card);

            if (previous != null) {
                throw new IllegalArgumentException(
                        "Повторяющийся ID карточки: " + card.id().value()
                );
            }
        }

        this.cards = Collections.unmodifiableMap(indexed);
        this.allCards = List.copyOf(indexed.values());
        this.starterCards = allCards.stream()
                .filter(CardTemplate::starter)
                .toList();
    }

    @Override
    public Optional<CardTemplate> find(TemplateId id) {
        Objects.requireNonNull(id, "id");
        return Optional.ofNullable(cards.get(id));
    }

    @Override
    public List<CardTemplate> all() {
        return allCards;
    }

    @Override
    public List<CardTemplate> starterSet() {
        return starterCards;
    }
}
