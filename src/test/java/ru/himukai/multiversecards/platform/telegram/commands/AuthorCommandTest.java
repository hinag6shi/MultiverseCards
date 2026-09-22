package ru.himukai.multiversecards.platform.telegram.commands;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuthorCommandTest {

    @Test
    void returnsNonEmptyAnswer() {
        String result = new AuthorCommand().execute(List.of());

        assertFalse(result.isBlank());
    }

    @Test
    void nameIsAuthor() {
        assertEquals("author", new AuthorCommand().name());
    }
}