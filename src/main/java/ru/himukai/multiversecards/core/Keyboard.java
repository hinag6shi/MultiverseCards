package ru.himukai.multiversecards.core;

import java.util.ArrayList;
import java.util.List;

public record Keyboard(List<List<Button>> rows) {

    public static final Keyboard NONE = new Keyboard(List.of());

    public Keyboard {
        rows = rows.stream().map(List::copyOf).toList();
    }

    /** Кнопки по рядам не более {@code perRow} штук в каждом ряде */
    public static Keyboard grid(List<Button> buttons, int perRow) {
        if (perRow < 1) {
            throw new IllegalArgumentException("perRow должно быть >= 1");
        }
        List<List<Button>> rows = new ArrayList<>();
        for (int i = 0; i < buttons.size(); i += perRow) {
            rows.add(buttons.subList(i, Math.min(i + perRow, buttons.size())));
        }
        return new Keyboard(rows);
    }

    public boolean isEmpty() {
        return rows.isEmpty();
    }
}
