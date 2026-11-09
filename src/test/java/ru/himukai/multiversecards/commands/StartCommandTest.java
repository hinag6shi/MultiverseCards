package ru.himukai.multiversecards.commands;

import org.junit.jupiter.api.Test;
import ru.himukai.multiversecards.core.CommandContext;
import ru.himukai.multiversecards.core.Response;
import ru.himukai.multiversecards.domain.player.CardCollection;
import ru.himukai.multiversecards.domain.player.Player;
import ru.himukai.multiversecards.domain.player.PlayerId;
import ru.himukai.multiversecards.service.PlayerService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StartCommandTest {

    private PlayerId lastId;
    private String lastName;

    private final PlayerService players = new PlayerService() {
        @Override
        public Player start(PlayerId id, String name) {
            lastId = id;
            lastName = name;
            return new Player(id, name, 0, new CardCollection());
        }

        @Override
        public Optional<Player> find(PlayerId id) {
            return Optional.empty();
        }

        @Override
        public Optional<Player> rename(PlayerId id, String name) {
            return Optional.empty();
        }
    };

    private final StartCommand command = new StartCommand(players);

    @Test
    void asksForNameWhenNoArgs() {
        Response response = command.execute(new CommandContext("tg:1", List.of()));

        assertTrue(response.text().contains("Как мне к тебе обращаться"));
        assertNull(lastId);
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
        assertEquals("вилка жопка", lastName);
    }

    @Test
    void registersPlayerUnderIdFromUserId() {
        command.execute(new CommandContext("tg:42", List.of("Имя")));

        assertEquals(new PlayerId(42), lastId);
        assertEquals("Имя", lastName);
    }

    @Test
    void malformedUserIdIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> command.execute(new CommandContext("tg:abc", List.of("Имя"))));
    }
}
