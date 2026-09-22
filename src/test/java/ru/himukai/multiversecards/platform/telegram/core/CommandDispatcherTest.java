package ru.himukai.multiversecards.platform.telegram.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

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
        assertEquals("args: [a, b]", dispatcher.handle("/echo a b"));
    }

    @Test
    void routesWithoutArgs() {
        assertEquals("args: []", dispatcher.handle("/echo"));
    }

    @Test
    void stripsBotNameAfterAtSign() {
        assertEquals("args: []", dispatcher.handle("/echo@MyCoolBot"));
    }

    @Test
    void isCaseInsensitive() {
        assertEquals("args: []", dispatcher.handle("/ECHO"));
    }

    @Test
    void unknownCommandGivesHint() {
        assertTrue(dispatcher.handle("/nope").contains("не найдена"));
    }

    @Test
    void textWithoutSlashIsRejected() {
        assertTrue(dispatcher.handle("привет").contains("слэша"));
    }

    @Test
    void blankTextIsRejected() {
        assertTrue(dispatcher.handle("   ").contains("Список команд"));
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
        public String execute(List<String> args) {
            return "args: " + args;
        }
    }
}