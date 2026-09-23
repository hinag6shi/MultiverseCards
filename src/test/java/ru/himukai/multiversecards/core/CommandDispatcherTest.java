package ru.himukai.multiversecards.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandDispatcherTest {

    private CommandDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        CommandRegistry registry = new CommandRegistry();
        registry.register(new EchoCommand());
        dispatcher = new CommandDispatcher(registry);
    }

    @Test
    void routesToMatchingCommandWithArgs() {
        assertEquals("args: [a, b]", dispatcher.handle("test", "/echo a b"));
    }

    @Test
    void routesWithoutArgs() {
        assertEquals("args: []", dispatcher.handle("test", "/echo"));
    }

    @Test
    void stripsBotNameAfterAtSign() {
        assertEquals("args: []", dispatcher.handle("test", "/echo@MyCoolBot"));
    }

    @Test
    void isCaseInsensitive() {
        assertEquals("args: []", dispatcher.handle("test", "/ECHO"));
    }

    @Test
    void unknownCommandGivesHint() {
        assertTrue(dispatcher.handle("test", "/nope").contains("не найдена"));
    }

    @Test
    void textWithoutSlashIsRejected() {
        assertTrue(dispatcher.handle("test", "привет").contains("слэша"));
    }

    @Test
    void blankTextIsRejected() {
        assertTrue(dispatcher.handle("test", "   ").contains("Список команд"));
    }

    private record EchoCommand() implements Command {
        @Override
        public String name() {
            return "echo";
        }

        @Override
        public String description() {
            return "повторяет аргументы";
        }

        @Override
        public String execute(CommandContext ctx) {
            return "args: " + ctx.args();
        }
    }
}