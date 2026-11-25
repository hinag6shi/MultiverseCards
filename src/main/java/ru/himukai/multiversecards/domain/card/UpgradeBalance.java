package ru.himukai.multiversecards.domain.card;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

/** Настройки баланса из upgrade-rules.json */
public record UpgradeBalance(
        int maxUpgradesPerCard,
        StatPercentages statIncreasePercent,
        GoldCost goldCost,
        Map<Rarity, Integer> shardCostByRarity,
        int shardsPerDuplicate
) {

    public UpgradeBalance {
        if (maxUpgradesPerCard < 0) {
            throw new IllegalArgumentException("maxUpgradesPerCard не может быть отрицательным");
        }
        Objects.requireNonNull(statIncreasePercent, "statIncreasePercent");
        Objects.requireNonNull(goldCost, "goldCost");
        Objects.requireNonNull(shardCostByRarity, "shardCostByRarity");
        if (shardsPerDuplicate < 0) {
            throw new IllegalArgumentException("shardsPerDuplicate не может быть отрицательным");
        }

        shardCostByRarity = Map.copyOf(shardCostByRarity);
        for (Rarity rarity : Rarity.values()) {
            Integer cost = shardCostByRarity.get(rarity);
            if (cost == null || cost < 0) {
                throw new IllegalArgumentException("Не задана корректная цена осколков для " + rarity);
            }
        }
    }

    public record StatPercentages(BigDecimal hp, BigDecimal st, BigDecimal df) {
        public StatPercentages {
            requirePercentage(hp, "hp");
            requirePercentage(st, "st");
            requirePercentage(df, "df");
        }

        private static void requirePercentage(BigDecimal value, String name) {
            if (value == null || value.signum() < 0) {
                throw new IllegalArgumentException("Процент " + name + " должен быть >= 0");
            }
        }
    }

    public record GoldCost(long base, long linear, long quadratic) {
        public GoldCost {
            if (base < 0 || linear < 0 || quadratic < 0) {
                throw new IllegalArgumentException("Коэффициенты цены золота долижны быть >= 0");
            }
        }

        /** Цена следующего апгрейда для карточки уровня n */
        public long forLevel(int level) {
            if (level < 0) {
                throw new IllegalArgumentException("Уровень усиления не может быть отрицательным");
            }
            return Math.addExact(base, Math.addExact(
                    Math.multiplyExact(linear, level),
                    Math.multiplyExact(quadratic, Math.multiplyExact((long) level, level))));
        }
    }
}
