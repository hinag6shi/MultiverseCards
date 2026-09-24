package ru.himukai.multiversecards.commands;

import org.junit.jupiter.api.Test;
import ru.himukai.multiversecards.core.CommandContext;
import ru.himukai.multiversecards.core.Response;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AboutCommandTest {

    @Test
    void mentionsGameName() {
        String result = ((Response.Text) new AboutCommand()
                .execute(new CommandContext("u1", List.of()))).text();

        assertTrue(result.contains("Multiverse Cards"));
    }

    @Test
    void nameAndDescriptionAreSet() {
        AboutCommand command = new AboutCommand();

        assertEquals("about", command.name());
        assertFalse(command.description().isBlank());
    }
}
