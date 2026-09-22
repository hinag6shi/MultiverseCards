package ru.himukai.multiversecards.platform.telegram.commands;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AboutCommandTest {

    @Test
    void mentionsGameName() {
        String result = new AboutCommand().execute(List.of());

        assertTrue(result.contains("Multiverse Cards"));
    }

    @Test
    void nameAndDescriptionAreSet() {
        AboutCommand command = new AboutCommand();

        assertEquals("about", command.name());
        assertFalse(command.description().isBlank());
    }
}