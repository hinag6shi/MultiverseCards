package ru.himukai.multiversecards.service;

import org.junit.jupiter.api.Test;
import ru.himukai.multiversecards.domain.card.Boosts;
import ru.himukai.multiversecards.domain.card.CardTemplate;
import ru.himukai.multiversecards.domain.card.Rarity;
import ru.himukai.multiversecards.domain.card.Stats;
import ru.himukai.multiversecards.domain.card.TemplateId;
import ru.himukai.multiversecards.domain.card.UpgradeBalance;
import ru.himukai.multiversecards.domain.card.UpgradePayment;
import ru.himukai.multiversecards.domain.card.UpgradeStat;
import ru.himukai.multiversecards.domain.player.CardCollection;
import ru.himukai.multiversecards.domain.player.OwnedCard;
import ru.himukai.multiversecards.domain.player.Player;
import ru.himukai.multiversecards.domain.player.PlayerId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CardUpgradeServiceTest {

    private static final PlayerId PLAYER_ID = new PlayerId(42);
    private static final TemplateId CARD_ID = new TemplateId("test_card");

    private static final class MemoryRepository implements PlayerRepository {
        private Player player;

        MemoryRepository() {
            CardTemplate template = new CardTemplate(CARD_ID, "Test", "Test universe",
                    Rarity.COMMON, new Stats(100, 20, 10), false);
            player = new Player(PLAYER_ID, "Player", 1_000,
                    new CardCollection(java.util.List.of(new OwnedCard(template, Boosts.NONE, 20, Instant.EPOCH))));
        }

        @Override
        public Optional<Player> find(PlayerId id) {
            return PLAYER_ID.equals(id) ? Optional.of(player) : Optional.empty();
        }

        @Override
        public void save(Player player) {
            this.player = player;
        }

        @Override
        public boolean grantStarterSetOnce(PlayerId id, Collection<OwnedCard> cards) {
            return false;
        }

        @Override
        public boolean upgradeCard(PlayerId id, TemplateId cardId, UpgradeStat stat, UpgradePayment payment,
                                   int expectedLevel, int maxLevel, long goldCost, int shardCost) {
            if (!PLAYER_ID.equals(id)) return false;
            OwnedCard card = player.cards().find(cardId).orElse(null);
            if (card == null || card.upgradeLevel() != expectedLevel || expectedLevel >= maxLevel) return false;
            if (payment == UpgradePayment.GOLD && player.cash() < goldCost) return false;
            if (payment == UpgradePayment.SHARDS && card.shards() < shardCost) return false;
            Boosts old = card.boosts();
            Boosts updated = switch (stat) {
                case HP -> new Boosts(old.hp() + 1, old.st(), old.df());
                case ST -> new Boosts(old.hp(), old.st() + 1, old.df());
                case DF -> new Boosts(old.hp(), old.st(), old.df() + 1);
            };
            CardCollection cards = new CardCollection(player.cards().all());
            cards.update(new OwnedCard(card.template(), updated,
                    payment == UpgradePayment.SHARDS ? card.shards() - shardCost : card.shards(), card.obtainedAt()));
            long cash = payment == UpgradePayment.GOLD ? player.cash() - goldCost : player.cash();
            player = new Player(id, player.name(), cash, cards, player.starterSetClaimed());
            return true;
        }

        @Override
        public boolean convertDuplicateToShards(PlayerId id, TemplateId cardId, int shardAmount) {
            if (!PLAYER_ID.equals(id)) return false;
            OwnedCard card = player.cards().find(cardId).orElse(null);
            if (card == null) return false;
            CardCollection cards = new CardCollection(player.cards().all());
            cards.update(card.withShards(Math.addExact(card.shards(), shardAmount)));
            player = new Player(id, player.name(), player.cash(), cards, player.starterSetClaimed());
            return true;
        }
    }

    private final UpgradeBalance balance = new UpgradeBalance(
            21,
            new UpgradeBalance.StatPercentages(new BigDecimal("0.025"),
                    new BigDecimal("0.035"), new BigDecimal("0.03")),
            new UpgradeBalance.GoldCost(100, 25, 10),
            Map.of(Rarity.COMMON, 20, Rarity.UNCOMMON, 17, Rarity.RARE, 14,
                    Rarity.EPIC, 11, Rarity.MYTHIC, 8, Rarity.LEGENDARY, 5, Rarity.DIVINE, 2),
            1);
    private final MemoryRepository repository = new MemoryRepository();
    private final CardUpgradeService service = new CardUpgradeService(repository, balance);

    @Test
    void upgradesStatAndDebitsGoldInOneRepositoryOperation() {
        assertTrue(service.upgrade(PLAYER_ID, CARD_ID, UpgradeStat.HP, UpgradePayment.GOLD));

        Player updated = repository.find(PLAYER_ID).orElseThrow();
        assertEquals(900, updated.cash());
        assertEquals(new Boosts(1, 0, 0), updated.cards().find(CARD_ID).orElseThrow().boosts());
    }

    @Test
    void shardUpgradeDebitsCardShardsAndDuplicateAddsOneShard() {
        assertTrue(service.upgrade(PLAYER_ID, CARD_ID, UpgradeStat.ST, UpgradePayment.SHARDS));
        assertTrue(service.convertDuplicateToShards(PLAYER_ID, CARD_ID));

        OwnedCard updated = repository.find(PLAYER_ID).orElseThrow().cards().find(CARD_ID).orElseThrow();
        assertEquals(1, updated.boosts().st());
        assertEquals(1, updated.shards());
    }

    @Test
    void rejectsUnknownCardAndDoesNotUpgradePastCardLimit() {
        assertFalse(service.upgrade(PLAYER_ID, new TemplateId("missing"), UpgradeStat.HP, UpgradePayment.GOLD));

        OwnedCard card = repository.find(PLAYER_ID).orElseThrow().cards().find(CARD_ID).orElseThrow();
        CardCollection cards = new CardCollection(java.util.List.of(card.withBoosts(new Boosts(21, 0, 0))));
        repository.save(new Player(PLAYER_ID, "Player", 1_000, cards));
        assertFalse(service.upgrade(PLAYER_ID, CARD_ID, UpgradeStat.HP, UpgradePayment.GOLD));
    }
}
