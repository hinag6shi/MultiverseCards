package ru.himukai.multiversecards.domain.card;

import java.util.Objects;
import java.util.UUID;

public record CardId(UUID value) {
    public CardId {
        Objects.requireNonNull(value, "value");
    }

    public static CardId random() {
        return new CardId(UUID.randomUUID());
    }
}