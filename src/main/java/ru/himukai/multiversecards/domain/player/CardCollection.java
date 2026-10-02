package ru.himukai.multiversecards.domain.player;

import ru.himukai.multiversecards.domain.card.Card;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CardCollection {
    private final List<Card> cards;

    public CardCollection() {
        this.cards = new ArrayList<>();
    }

    public CardCollection(List<Card> cards) {
        this.cards = new ArrayList<>(
                Objects.requireNonNull(cards, "cards")
        );
    }

    public void add(Card card) {
        cards.add(Objects.requireNonNull(card, "card"));
    }

    public List<Card> all() {
        return List.copyOf(cards);
    }

    public int size() {
        return cards.size();
    }
}