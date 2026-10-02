package ru.himukai.multiversecards.domain.player;

public record PlayerId(long value) {
    public PlayerId {
        if (value <= 0) {
            throw new IllegalArgumentException("id <= 0");
        }
    }
}