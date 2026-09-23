package ru.himukai.multiversecards.commands;

import org.junit.jupiter.api.Test;
import ru.himukai.multiversecards.core.CommandContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AboutCommandTest {

    @Test
    void mentionsGameName() {
        CommandContext ctx = new CommandContext("test", List.of());
        String result = new AboutCommand().execute(ctx);

        assertTrue(result.contains("Multiverse Cards"));
    }

    @Test
    void nameAndDescriptionAreSet() {
        AboutCommand command = new AboutCommand();

        assertEquals("about", command.name());
        assertFalse(command.description().isBlank());
    }
}