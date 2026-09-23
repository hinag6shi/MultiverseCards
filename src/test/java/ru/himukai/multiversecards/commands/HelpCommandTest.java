package ru.himukai.multiversecards.commands;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.himukai.multiversecards.core.Command;
import ru.himukai.multiversecards.core.CommandContext;
import ru.himukai.multiversecards.core.CommandRegistry;

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
        CommandContext ctx = new CommandContext("test", List.of());
        String result = help.execute(ctx);

        assertTrue(result.contains("/help"));
        assertTrue(result.contains("/ping — проверка связи"));
    }

    @Test
    void newlyRegisteredCommandAppearsAutomatically() {
        registry.register(new StubCommand("later", "добавлена позже"));

        CommandContext ctx = new CommandContext("test", List.of());
        assertTrue(help.execute(ctx).contains("/later — добавлена позже"));
    }

    @Test
    void withArgShowsDetailsOfThatCommand() {
        CommandContext ctx = new CommandContext("test", List.of("ping"));
        String result = help.execute(ctx);

        assertEquals("/ping\nпроверка связи", result);
    }

    @Test
    void argWithSlashAndDifferentCaseIsAccepted() {
        CommandContext ctx1 = new CommandContext("test", List.of("ping"));
        CommandContext ctx2 = new CommandContext("test", List.of("/PING"));
        assertEquals(help.execute(ctx1), help.execute(ctx2));
    }

    @Test
    void unknownCommandGivesHint() {
        CommandContext ctx = new CommandContext("test", List.of("nope"));
        String result = help.execute(ctx);

        assertTrue(result.contains("не найдена"));
    }

    private record StubCommand(String name, String description) implements Command {
        @Override
        public String execute(CommandContext ctx) {
            return "";
        }
    }
}
