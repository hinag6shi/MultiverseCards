package ru.himukai.multiversecards.commands;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.himukai.multiversecards.core.Command;
import ru.himukai.multiversecards.core.CommandContext;
import ru.himukai.multiversecards.core.CommandRegistry;
import ru.himukai.multiversecards.core.Response;

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
        String result = text(help.execute(ctx()));

        assertTrue(result.contains("/help"));
        assertTrue(result.contains("/ping — проверка связи"));
    }

    @Test
    void newlyRegisteredCommandAppearsAutomatically() {
        registry.register(new StubCommand("later", "добавлена позже"));

        assertTrue(text(help.execute(ctx())).contains("/later — добавлена позже"));
    }

    @Test
    void withArgShowsDetailsOfThatCommand() {
        String result = text(help.execute(ctx("ping")));

        assertEquals("/ping\nпроверка связи", result);
    }

    @Test
    void argWithSlashAndDifferentCaseIsAccepted() {
        assertEquals(text(help.execute(ctx("ping"))), text(help.execute(ctx("/PING"))));
    }

    @Test
    void unknownCommandGivesHint() {
        String result = text(help.execute(ctx("nope")));

        assertTrue(result.contains("не найдена"));
    }

    private static CommandContext ctx(String... args) {
        return new CommandContext("u1", List.of(args));
    }

    private static String text(Response response) {
        return ((Response.Text) response).text();
    }

    private record StubCommand(String name, String description) implements Command {
        @Override
        public Response execute(CommandContext ctx) {
            return Response.text("");
        }
    }
}
