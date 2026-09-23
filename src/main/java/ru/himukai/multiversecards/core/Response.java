package ru.himukai.multiversecards.core;

import java.util.List;

public sealed interface Response {

    record Text(String text, List<Button> buttons) implements Response {

        public Text(String text) {
            this(text, List.of());
        }
    }

    static Response text(String text) {
        return new Text(text);
    }

    static Response text(String text, List<Button> buttons) {
        return new Text(text, buttons);
    }
}