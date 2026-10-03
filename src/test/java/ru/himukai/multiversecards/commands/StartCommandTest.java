package ru.himukai.multiversecards.commands;

import org.junit.jupiter.api.Test;
import ru.himukai.multiversecards.core.CommandContext;
import ru.himukai.multiversecards.core.Response;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class StartCommandTest {

    private final StartCommand command = new StartCommand();

    @Test
    void asksForNameWhenNoArgs() {
        Response response = command.execute(new CommandContext("tg:1", List.of()));

        assertTrue(response.text().contains("Как мне к тебе обращаться"));
    }

    @Test
    void greetsByNameWhenGiven() {
        Response response = command.execute(new CommandContext("tg:1", List.of("вилка")));

        assertTrue(response.text().contains("вилка"));
    }

    @Test
    void joinsSeveralWordsIntoOneName() {
        Response response = command.execute(new CommandContext("tg:1", List.of("вилка", "жопка")));

        assertTrue(response.text().contains("вилка жопка"));
    }
}