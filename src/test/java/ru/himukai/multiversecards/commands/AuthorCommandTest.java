package ru.himukai.multiversecards.commands;

import org.junit.jupiter.api.Test;
import ru.himukai.multiversecards.core.CommandContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuthorCommandTest {

    @Test
    void returnsNonEmptyAnswer() {
        CommandContext ctx = new CommandContext("test", List.of());
        String result = new AuthorCommand().execute(ctx);

        assertFalse(result.isBlank());
    }

    @Test
    void nameIsAuthor() {
        assertEquals("author", new AuthorCommand().name());
    }
}