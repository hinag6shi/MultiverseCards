package ru.himukai.multiversecards.domain.card;

public record Stats(int hp, int st, int df) {
    public Stats {
        if (hp < 0) {
            throw new IllegalArgumentException("hp не может быть отрицательным");
        }
        if (st < 0) {
            throw new IllegalArgumentException("st не может быть отрицательным");
        }
        if (df < 0) {
            throw new IllegalArgumentException("df не может быть отрицательным");
        }
    }
}