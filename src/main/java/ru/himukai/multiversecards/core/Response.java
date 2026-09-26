// core/Response.java
package ru.himukai.multiversecards.core;

import java.util.List;

public sealed interface Response {

    Button.Keyboard keyboard();
    RenderMode renderMode();

    enum RenderMode { NEW, UPDATE }

    record Text(String text, Button.Keyboard keyboard, RenderMode renderMode) implements Response {
        public Text(String text) {
            this(text, Button.Keyboard.NONE, RenderMode.NEW);
        }
    }

    static Response text(String text) {
        return new Text(text);
    }

    static Response text(String text, Button.Keyboard keyboard) {
        return new Text(text, keyboard, RenderMode.NEW);
    }

    static Response update(String text, Button.Keyboard keyboard) {
        return new Text(text, keyboard, RenderMode.UPDATE);
    }
}