package ru.himukai.multiversecards.domain.card;

/** Сколько апгрейдов уже вложено в каждый стат этой карточки */
public record Boosts(int hp, int st, int df) {

    public static final Boosts NONE = new Boosts(0, 0, 0);

    public Boosts {
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
