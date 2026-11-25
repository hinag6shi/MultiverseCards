package ru.himukai.multiversecards.domain.card;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ru.himukai.multiversecards.infra.config.UpgradeBalanceLoader;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpgradeRulesTest {

    private final UpgradeBalance balance = new UpgradeBalance(
            21,
            new UpgradeBalance.StatPercentages(new BigDecimal("0.025"),
                    new BigDecimal("0.035"), new BigDecimal("0.03")),
            new UpgradeBalance.GoldCost(100, 25, 10),
            Map.of(Rarity.COMMON, 20, Rarity.UNCOMMON, 17, Rarity.RARE, 14,
                    Rarity.EPIC, 11, Rarity.MYTHIC, 8, Rarity.LEGENDARY, 5, Rarity.DIVINE, 2),
            1);

    @Test
    void appliesRoundedPerUpgradeIncrementsToBaseStats() {
        UpgradeRules rules = new UpgradeRules(balance);

        Stats actual = rules.upgraded(new Stats(100, 10, 0), new Boosts(2, 3, 4));

        assertEquals(new Stats(106, 13, 0), actual);
    }

    @Test
    void capsTotalUpgradesAcrossAllStatsAtTwentyOne() {
        UpgradeRules rules = new UpgradeRules(balance);

        assertTrue(rules.upgradeAllowed(new Boosts(20, 0, 0)));
        assertFalse(rules.upgradeAllowed(new Boosts(7, 7, 7)));
        assertEquals(21, rules.totalUpgrades(new Boosts(8, 6, 7)));
    }

    @Test
    void calculatesGoldFromCurrentCardLevelAndUsesFixedRarityShardPrices() {
        UpgradeRules rules = new UpgradeRules(balance);

        assertEquals(190, rules.goldCost(2));
        assertEquals(20, rules.shardCost(Rarity.COMMON));
        assertEquals(2, rules.shardCost(Rarity.DIVINE));
        assertEquals(1, rules.shardsPerDuplicate());
    }

    @Test
    void loadsBalanceConfigurationFromResources() {
        UpgradeBalance loaded = UpgradeBalanceLoader.load(new ObjectMapper());

        assertEquals(21, loaded.maxUpgradesPerCard());
        assertEquals(2, loaded.shardCostByRarity().get(Rarity.DIVINE));
        assertEquals(1, loaded.shardsPerDuplicate());
    }
}
