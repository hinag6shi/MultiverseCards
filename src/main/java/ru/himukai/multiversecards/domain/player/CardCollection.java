package ru.himukai.multiversecards.domain.player;

import ru.himukai.multiversecards.domain.card.TemplateId;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Коллекция игрока: не более одной карточки на каждый шаблон, порядок добавления сохраняется. */
public final class CardCollection {
    private final Map<TemplateId, OwnedCard> cards = new LinkedHashMap<>();

    public CardCollection() {
    }

    public CardCollection(Collection<OwnedCard> cards) {
        Objects.requireNonNull(cards, "cards").forEach(this::add);
    }

    /** @throws IllegalArgumentException если такая карточка уже есть (повтор обрабатывается снаружи) */
    public void add(OwnedCard card) {
        Objects.requireNonNull(card, "card");
        if (cards.putIfAbsent(card.templateId(), card) != null) {
            throw new IllegalArgumentException("Карточка уже есть в коллекции: " + card.templateId().value());
        }
    }

    /** Заменяет существующую карточку (например, после усиления или начисления осколков). */
    public void update(OwnedCard card) {
        Objects.requireNonNull(card, "card");
        if (!cards.containsKey(card.templateId())) {
            throw new IllegalArgumentException("Карточки нет в коллекции: " + card.templateId().value());
        }
        cards.put(card.templateId(), card);
    }

    public Optional<OwnedCard> find(TemplateId id) {
        return Optional.ofNullable(cards.get(id));
    }

    public boolean contains(TemplateId id) {
        return cards.containsKey(id);
    }

    public List<OwnedCard> all() {
        return List.copyOf(cards.values());
    }

    public int size() {
        return cards.size();
    }
}
