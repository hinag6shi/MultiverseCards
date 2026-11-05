package ru.himukai.multiversecards.service;

import org.junit.jupiter.api.Test;
import ru.himukai.multiversecards.domain.player.CardCollection;
import ru.himukai.multiversecards.domain.player.Player;
import ru.himukai.multiversecards.domain.player.PlayerId;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultPlayerServiceTest {

    private static final class InMemoryRepository implements PlayerRepository {
        final Map<PlayerId, Player> data = new HashMap<>();

        @Override
        public Optional<Player> find(PlayerId id) {
            return Optional.ofNullable(data.get(id));
        }

        @Override
        public void save(Player player) {
            data.put(player.id(), player);
        }
    }

    private final InMemoryRepository repository = new InMemoryRepository();
    private final DefaultPlayerService service = new DefaultPlayerService(repository);

    @Test
    void createsNewPlayerWithEmptyWalletAndCollection() {
        Player player = service.start(new PlayerId(1), "Вилка");

        assertEquals("Вилка", player.name());
        assertEquals(0, player.cash());
        assertEquals(0, player.cards().size());
        assertSame(player, repository.data.get(new PlayerId(1)));
    }

    @Test
    void existingPlayerIsRenamedAndKeepsCash() {
        repository.save(new Player(new PlayerId(1), "Старое", 50, new CardCollection()));

        Player player = service.start(new PlayerId(1), "Новое");

        assertEquals("Новое", player.name());
        assertEquals(50, player.cash());
        assertEquals(1, repository.data.size());
    }

    @Test
    void blankNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.start(new PlayerId(1), " "));
    }
}
