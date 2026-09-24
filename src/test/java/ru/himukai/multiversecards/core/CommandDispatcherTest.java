package ru.himukai.multiversecards.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CommandDispatcherTest {

    private CommandDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        CommandRegistry registry = new CommandRegistry();
        registry.register(new EchoCommand());
        dispatcher = new CommandDispatcher(registry);
    }

    @Test
    void routesToMatchingCommandWithArgsAndUserId() {
        assertEquals("user=u1 args=[a, b]", text(dispatcher.handle("u1", "/echo a b")));
    }

    @Test
    void routesWithoutArgs() {
        assertEquals("user=u1 args=[]", text(dispatcher.handle("u1", "/echo")));
    }

    @Test
    void stripsBotNameAfterAtSign() {
        assertEquals("user=u1 args=[]", text(dispatcher.handle("u1", "/echo@MyCoolBot")));
    }

    @Test
    void isCaseInsensitive() {
        assertEquals("user=u1 args=[]", text(dispatcher.handle("u1", "/ECHO")));
    }

    @Test
    void unknownCommandGivesHint() {
        assertTrue(text(dispatcher.handle("u1", "/nope")).contains("не найдена"));
    }

    @Test
    void textWithoutSlashIsRejected() {
        assertTrue(text(dispatcher.handle("u1", "привет")).contains("слэша"));
    }

    @Test
    void blankTextIsRejected() {
        assertTrue(text(dispatcher.handle("u1", "   ")).contains("Список команд"));
    }

    private static String text(Response response) {
        return ((Response.Text) response).text();
    }

    private record EchoCommand() implements Command {
        @Override
        public String name() {
            return "echo";
        }

        @Override
        public String description() {
            return "повторяет userId и аргументы";
        }

        @Override
        public Response execute(CommandContext ctx) {
            return Response.text("user=" + ctx.userId() + " args=" + ctx.args());
        }
    }
}
