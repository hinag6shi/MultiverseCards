package ru.himukai.multiversecards.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResponseTest {

    private static final Keyboard KEYBOARD =
            Keyboard.grid(List.of(Button.command("Помощь", "help")), 1);

    @Test
    void textIsNewMessageWithoutKeyboard() {
        Response response = Response.text("привет");

        assertEquals(Response.RenderMode.NEW, response.renderMode());
        assertSame(Keyboard.NONE, response.keyboard());
    }

    @Test
    void textWithKeyboardIsNewMessage() {
        Response response = Response.text("привет", KEYBOARD);

        assertEquals(Response.RenderMode.NEW, response.renderMode());
        assertSame(KEYBOARD, response.keyboard());
    }

    @Test
    void updateKeepsKeyboardAndMode() {
        Response response = Response.update("привет", KEYBOARD);

        assertEquals(Response.RenderMode.UPDATE, response.renderMode());
        assertSame(KEYBOARD, response.keyboard());
    }

    @Test
    void nullKeyboardBecomesNone() {
        assertSame(Keyboard.NONE, Response.text("x", null).keyboard());
    }

    @Test
    void nullTextIsRejected() {
        assertThrows(NullPointerException.class, () -> Response.text(null));
    }
}
