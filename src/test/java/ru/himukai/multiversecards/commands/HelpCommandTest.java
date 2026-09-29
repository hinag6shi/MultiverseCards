package ru.himukai.multiversecards.commands;

import org.junit.jupiter.api.Test;
import ru.himukai.multiversecards.core.Button;
import ru.himukai.multiversecards.core.CommandContext;
import ru.himukai.multiversecards.core.CommandRegistry;
import ru.himukai.multiversecards.core.Response;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HelpCommandTest {

    private final CommandRegistry registry = new CommandRegistry();
    private final HelpCommand help = new HelpCommand(registry);

    HelpCommandTest() {
        registry.register(help, new AuthorCommand(), new AboutCommand());
    }

    private Response run(String... args) {
        return help.execute(new CommandContext("u", List.of(args)));
    }

    @Test
    void withoutArgsListsAllCommandsAlphabetically() {
        String text = run().text();

        assertTrue(text.startsWith("Доступные команды:"));
        int about = text.indexOf("/about");
        int author = text.indexOf("/author");
        int helpPos = text.indexOf("/help —");
        assertTrue(about >= 0 && about < author && author < helpPos);
        assertTrue(text.contains("Информация об авторах бота"));
    }

    @Test
    void overviewHasOneButtonPerCommandLeadingToDetails() {
        Response response = run();

        List<String> commandTexts = response.keyboard().rows().stream()
                .flatMap(List::stream).map(Button::commandText).toList();

        assertEquals(List.of("/help about", "/help author", "/help help"), commandTexts);
        assertEquals(Response.RenderMode.UPDATE, response.renderMode());
    }

    @Test
    void withArgShowsUsageAndDescription() {
        Response response = run("author");

        assertEquals("/author\nИнформация об авторах бота", response.text());
    }

    @Test
    void argWithSlashAndDifferentCaseIsAccepted() {
        assertEquals(run("author").text(), run("/AUTHOR").text());
    }

    @Test
    void helpOnHelpUsesCustomUsage() {
        assertTrue(run("help").text().startsWith("/help [команда]"));
    }

    @Test
    void detailsHaveBackButtonToOverview() {
        Response response = run("about");

        assertEquals(1, response.keyboard().rows().size());
        assertEquals("/help", response.keyboard().rows().getFirst().getFirst().commandText());
        assertEquals(Response.RenderMode.UPDATE, response.renderMode());
    }

    @Test
    void unknownCommandIsReportedAsNewMessage() {
        Response response = run("nope");

        assertTrue(response.text().contains("«nope»"));
        assertTrue(response.text().contains("/help"));
        assertEquals(Response.RenderMode.NEW, response.renderMode());
        assertTrue(response.keyboard().isEmpty());
    }

    @Test
    void extraArgsAreIgnored() {
        assertEquals(run("author").text(), run("author", "лишнее").text());
    }
}
