package ru.himukai.multiversecards.core;

import java.util.List;

public record Button(String label, String commandText) {

    public record Keyboard(List<List<Button>> rows) {
        public static final Keyboard NONE = new Keyboard(List.of());

        public static Keyboard grid(List<Button> buttons, int perRow) {
            List<List<Button>> rows = new java.util.ArrayList<>();
            for (int i = 0; i < buttons.size(); i += perRow) {
                rows.add(buttons.subList(i, Math.min(i + perRow, buttons.size())));
            }
            return new Keyboard(rows);
        }

        public boolean isEmpty() { return rows.isEmpty(); }
    }
}