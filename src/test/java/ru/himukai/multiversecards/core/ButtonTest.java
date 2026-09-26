package ru.himukai.multiversecards.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ButtonTest {

    @Test
    void noneIsEmpty() {
        assertTrue(Button.Keyboard.NONE.isEmpty());
    }

    @Test
    void gridSplitsButtonsIntoRowsOfGivenSize() {
        List<Button> buttons = List.of(
                new Button("1", "/a"), new Button("2", "/b"), new Button("3", "/c")
        );

        Button.Keyboard keyboard = Button.Keyboard.grid(buttons, 2);

        assertEquals(2, keyboard.rows().size());
        assertEquals(2, keyboard.rows().get(0).size());
        assertEquals(1, keyboard.rows().get(1).size());
        assertEquals("3", keyboard.rows().get(1).getFirst().label());
    }

    @Test
    void gridWithExactMultipleLeavesNoPartialRow() {
        List<Button> buttons = List.of(new Button("1", "/a"), new Button("2", "/b"));

        assertEquals(1, Button.Keyboard.grid(buttons, 2).rows().size());
    }

    @Test
    void gridOfEmptyListIsEmpty() {
        assertTrue(Button.Keyboard.grid(List.of(), 3).isEmpty());
    }
}