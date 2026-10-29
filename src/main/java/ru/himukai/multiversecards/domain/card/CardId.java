package ru.himukai.multiversecards.domain.card;

import java.util.UUID;

public record CardId(UUID value) {
    public static CardId random() {
        return new CardId(UUID.randomUUID());
    }
}