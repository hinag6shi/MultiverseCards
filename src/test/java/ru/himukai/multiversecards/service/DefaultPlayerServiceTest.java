package ru.himukai.multiversecards.service;

import org.junit.jupiter.api.Test;
import ru.himukai.multiversecards.domain.card.CardTemplate;
import ru.himukai.multiversecards.domain.card.Rarity;
import ru.himukai.multiversecards.domain.card.Stats;
import ru.himukai.multiversecards.domain.card.TemplateId;
import ru.himukai.multiversecards.domain.card.UpgradePayment;
import ru.himukai.multiversecards.domain.card.UpgradeStat;
import ru.himukai.multiversecards.domain.player.CardCollection;
import ru.himukai.multiversecards.domain.player.OwnedCard;
import ru.himukai.multiversecards.domain.player.Player;
import ru.himukai.multiversecards.domain.player.PlayerId;

import java.util.HashMap;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        @Override
        public boolean grantStarterSetOnce(PlayerId id, Collection<ru.himukai.multiversecards.domain.player.OwnedCard> cards) {
            Player current = data.get(id);
            if (current == null || current.starterSetClaimed()) return false;
            List<OwnedCard> allCards = new ArrayList<>(current.cards().all());
            allCards.addAll(cards);
            data.put(id, new Player(id, current.name(), current.cash(), new CardCollection(allCards), true));
            return true;
        }

        @Override
        public boolean upgradeCard(PlayerId id, TemplateId cardId, UpgradeStat stat, UpgradePayment payment,
                                   int expectedLevel, int maxLevel, long goldCost, int shardCost) {
            Player player = data.get(id);
            if (player == null) return false;
            OwnedCard card = player.cards().find(cardId).orElse(null);
            if (card == null || card.upgradeLevel() != expectedLevel || expectedLevel >= maxLevel) return false;
            if (payment == UpgradePayment.GOLD && player.cash() < goldCost) return false;
            if (payment == UpgradePayment.SHARDS && card.shards() < shardCost) return false;
            var old = card.boosts();
            var boosts = switch (stat) {
                case HP -> new ru.himukai.multiversecards.domain.card.Boosts(old.hp() + 1, old.st(), old.df());
                case ST -> new ru.himukai.multiversecards.domain.card.Boosts(old.hp(), old.st() + 1, old.df());
                case DF -> new ru.himukai.multiversecards.domain.card.Boosts(old.hp(), old.st(), old.df() + 1);
            };
            var cards = new CardCollection(player.cards().all());
            cards.update(new OwnedCard(card.template(), boosts,
                    payment == UpgradePayment.SHARDS ? card.shards() - shardCost : card.shards(), card.obtainedAt()));
            data.put(id, new Player(id, player.name(),
                    payment == UpgradePayment.GOLD ? player.cash() - goldCost : player.cash(), cards,
                    player.starterSetClaimed()));
            return true;
        }

        @Override
        public boolean convertDuplicateToShards(PlayerId id, TemplateId cardId, int shardAmount) {
            Player player = data.get(id);
            if (player == null) return false;
            OwnedCard card = player.cards().find(cardId).orElse(null);
            if (card == null) return false;
            var cards = new CardCollection(player.cards().all());
            cards.update(card.withShards(Math.addExact(card.shards(), shardAmount)));
            data.put(id, new Player(id, player.name(), player.cash(), cards, player.starterSetClaimed()));
            return true;
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
        assertTrue(repository.data.get(new PlayerId(1)).starterSetClaimed());
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

    @Test
    void grantsStarterCardsOnlyOnce() {
        CardTemplate starter = new CardTemplate(new TemplateId("starter"), "Starter", "Test",
                Rarity.COMMON, new Stats(10, 5, 3), true);
        DefaultPlayerService withStarter = new DefaultPlayerService(repository,
                new DefaultCardCatalog(List.of(starter)));

        Player firstStart = withStarter.start(new PlayerId(2), "Игрок");
        Player repeatedStart = withStarter.start(new PlayerId(2), "Игрок");

        assertTrue(firstStart.starterSetClaimed());
        assertEquals(1, firstStart.cards().size());
        assertTrue(repeatedStart.starterSetClaimed());
        assertEquals(1, repeatedStart.cards().size());
    }

    @Test
    void renameUpdatesStoredName() {
        service.start(new PlayerId(3), "Старое имя");

        Player renamed = service.rename(new PlayerId(3), "Новое имя").orElseThrow();

        assertEquals("Новое имя", renamed.name());
        assertEquals("Новое имя", repository.find(new PlayerId(3)).orElseThrow().name());
    }
}
