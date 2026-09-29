package ru.himukai.multiversecards.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CommandDispatcherTest {

    private final StubCommand echo = new StubCommand("echo", ctx -> Response.text("echo:" + ctx.args()));
    private final CommandDispatcher dispatcher =
            new CommandDispatcher(new CommandRegistry().register(echo));

    @Test
    void nullAndBlankTextGiveHint() {
        assertTrue(dispatcher.handle("u", null).text().contains("/help"));
        assertTrue(dispatcher.handle("u", "").text().contains("/help"));
        assertTrue(dispatcher.handle("u", "   \n ").text().contains("/help"));
    }

    @Test
    void textWithoutSlashIsRejected() {
        Response response = dispatcher.handle("u", "привет");

        assertTrue(response.text().contains("слэша"));
        assertNull(echo.lastContext());
    }

    @Test
    void emptyCommandNameIsRejected() {
        assertTrue(dispatcher.handle("u", "/").text().contains("Не указана команда"));
        assertTrue(dispatcher.handle("u", "/@MyBot").text().contains("Не указана команда"));
    }

    @Test
    void unknownCommandMentionsItsName() {
        Response response = dispatcher.handle("u", "/nope arg");

        assertTrue(response.text().contains("/nope"));
        assertTrue(response.text().contains("не найдена"));
    }

    @Test
    void knownCommandReceivesUserIdAndArgs() {
        Response response = dispatcher.handle("tg:42", "/echo a b");

        assertEquals("echo:[a, b]", response.text());
        assertEquals("tg:42", echo.lastContext().userId());
        assertEquals(List.of("a", "b"), echo.lastContext().args());
    }

    @Test
    void commandWithoutArgsGetsEmptyList() {
        dispatcher.handle("u", "/echo");

        assertEquals(List.of(), echo.lastContext().args());
    }

    @Test
    void botSuffixCaseAndExtraWhitespaceAreHandled() {
        dispatcher.handle("u", "  /ECHO@MyBot   x \t y  ");

        assertEquals(List.of("x", "y"), echo.lastContext().args());
    }

    @Test
    void commandExceptionIsCaughtAndUserGetsFriendlyReply() {
        StubCommand broken = new StubCommand("broken", ctx -> {
            throw new IllegalStateException("boom");
        });
        CommandDispatcher safe = new CommandDispatcher(new CommandRegistry().register(broken));

        Response response = safe.handle("u", "/broken");

        assertTrue(response.text().contains("Не удалось выполнить команду"));
        assertFalse(response.text().contains("boom"));
    }
}
