package ru.himukai.multiversecards.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyboardTest {

    private static List<Button> buttons(int count) {
        List<Button> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            list.add(Button.command("b" + i, "cmd", String.valueOf(i)));
        }
        return list;
    }

    @Test
    void commandButtonBuildsCommandText() {
        assertEquals("/help", Button.command("Все", "help").commandText());
        assertEquals("/help author", Button.command("Автор", "help", "author").commandText());
        assertEquals("/a x y", Button.command("A", "a", "x", "y").commandText());
    }

    @Test
    void buttonRejectsBlankFields() {
        assertThrows(IllegalArgumentException.class, () -> new Button(" ", "/help"));
        assertThrows(IllegalArgumentException.class, () -> new Button("Помощь", ""));
        assertThrows(NullPointerException.class, () -> new Button(null, "/help"));
    }

    @Test
    void gridSplitsButtonsIntoRows() {
        Keyboard keyboard = Keyboard.grid(buttons(5), 2);

        assertEquals(List.of(2, 2, 1), keyboard.rows().stream().map(List::size).toList());
        assertEquals("/cmd 5", keyboard.rows().get(2).getFirst().commandText());
    }

    @Test
    void gridRejectsNonPositiveRowSize() {
        assertThrows(IllegalArgumentException.class, () -> Keyboard.grid(buttons(3), 0));
    }

    @Test
    void emptyInputGivesEmptyKeyboard() {
        assertTrue(Keyboard.grid(List.of(), 3).isEmpty());
        assertTrue(Keyboard.NONE.isEmpty());
        assertFalse(Keyboard.grid(buttons(1), 3).isEmpty());
    }

    @Test
    void keyboardIsNotAffectedByLaterChangesOfSourceList() {
        List<Button> source = buttons(2);
        Keyboard keyboard = Keyboard.grid(source, 2);

        source.clear();

        assertEquals(2, keyboard.rows().getFirst().size());
        assertThrows(UnsupportedOperationException.class, () -> keyboard.rows().clear());
    }
}
