package ru.himukai.platform.telegram.commands;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.himukai.platform.telegram.core.Command;
import ru.himukai.platform.telegram.core.CommandRegistry;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HelpCommandTest {

    private CommandRegistry registry;
    private HelpCommand help;

    @BeforeEach
    void setUp() {
        registry = new CommandRegistry();
        help = new HelpCommand(registry);
        registry.register(help, new StubCommand("ping", "проверка связи"));
    }

    @Test
    void withoutArgsListsAllCommands() {
        String result = help.execute(List.of());

        assertTrue(result.contains("/help"));
        assertTrue(result.contains("/ping — проверка связи"));
    }

    @Test
    void newlyRegisteredCommandAppearsAutomatically() {
        registry.register(new StubCommand("later", "добавлена позже"));

        assertTrue(help.execute(List.of()).contains("/later — добавлена позже"));
    }

    @Test
    void withArgShowsDetailsOfThatCommand() {
        String result = help.execute(List.of("ping"));

        assertEquals("/ping\nпроверка связи", result);
    }

    @Test
    void argWithSlashAndDifferentCaseIsAccepted() {
        assertEquals(help.execute(List.of("ping")), help.execute(List.of("/PING")));
    }

    @Test
    void unknownCommandGivesHint() {
        String result = help.execute(List.of("nope"));

        assertTrue(result.contains("не найдена"));
    }

    private record StubCommand(String name, String description) implements Command {
        @Override
        public String execute(List<String> args) {
            return "";
        }
    }
}
