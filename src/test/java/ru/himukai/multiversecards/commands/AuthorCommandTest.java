package ru.himukai.multiversecards.commands;

import org.junit.jupiter.api.Test;
import ru.himukai.multiversecards.core.CommandContext;
import ru.himukai.multiversecards.core.Response;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuthorCommandTest {

    @Test
    void returnsNonEmptyAnswer() {
        String result = ((Response.Text) new AuthorCommand()
                .execute(new CommandContext("u1", List.of()))).text();

        assertFalse(result.isBlank());
    }

    @Test
    void nameIsAuthor() {
        assertEquals("author", new AuthorCommand().name());
    }
}
