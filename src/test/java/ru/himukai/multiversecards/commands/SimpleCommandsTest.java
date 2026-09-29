package ru.himukai.multiversecards.commands;

import org.junit.jupiter.api.Test;
import ru.himukai.multiversecards.core.Command;
import ru.himukai.multiversecards.core.CommandContext;
import ru.himukai.multiversecards.core.Response;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleCommandsTest {

    private static final CommandContext CTX = new CommandContext("u", List.of());

    @Test
    void authorNamesBothAuthors() {
        Response response = new AuthorCommand().execute(CTX);

        assertTrue(response.text().contains("@hinag6shi"));
        assertTrue(response.text().contains("@oleacs"));
        assertTrue(response.keyboard().isEmpty());
    }

    @Test
    void aboutDescribesTheGame() {
        Response response = new AboutCommand().execute(CTX);

        assertTrue(response.text().contains("Multiverse Cards"));
        assertTrue(response.keyboard().isEmpty());
    }

    @Test
    void metadataIsFilledAndNamesAreLowerCaseWithoutSlash() {
        for (Command command : List.of(new AuthorCommand(), new AboutCommand())) {
            assertFalse(command.description().isBlank());
            assertEquals(command.name().toLowerCase(), command.name());
            assertFalse(command.name().startsWith("/"));
            assertEquals("/" + command.name(), command.usage());
        }
    }
}
