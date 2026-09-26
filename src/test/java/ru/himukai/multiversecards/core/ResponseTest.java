package ru.himukai.multiversecards.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ResponseTest {

    @Test
    void plainTextHasNoKeyboardAndIsNewMessage() {
        Response.Text response = (Response.Text) Response.text("hello");

        assertEquals("hello", response.text());
        assertTrue(response.keyboard().isEmpty());
        assertEquals(Response.RenderMode.NEW, response.renderMode());
    }

    @Test
    void textWithKeyboardKeepsNewMode() {
        Button.Keyboard keyboard = Button.Keyboard.grid(List.of(new Button("ok", "/ok")), 1);

        Response.Text response = (Response.Text) Response.text("choose", keyboard);

        assertEquals(keyboard, response.keyboard());
        assertEquals(Response.RenderMode.NEW, response.renderMode());
    }

    @Test
    void updateSetsRenderModeToUpdate() {
        Response.Text response = (Response.Text) Response.update("step 2", Button.Keyboard.NONE);

        assertEquals(Response.RenderMode.UPDATE, response.renderMode());
    }
}