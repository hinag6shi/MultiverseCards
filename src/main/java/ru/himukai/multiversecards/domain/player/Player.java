package ru.himukai.multiversecards.domain.player;

import java.util.Objects;

public final class Player {
    private final PlayerId id;
    private String name;
    private long cash;
    private final CardCollection cards;

    public Player(
            PlayerId id,
            String name,
            long cash,
            CardCollection cards
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.cards = Objects.requireNonNull(cards, "cards");

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name не может быть пустым");
        }

        if (cash < 0) {
            throw new IllegalArgumentException("cash не может быть отрицательным");
        }

        this.name = name;
        this.cash = cash;
    }

    public PlayerId id() {
        return id;
    }

    public String name() {
        return name;
    }

    public long cash() {
        return cash;
    }

    public CardCollection cards() {
        return cards;
    }

    public void rename(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name не может быть пустым");
        }

        this.name = name;
    }
}