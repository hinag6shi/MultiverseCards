package ru.himukai.multiversecards.domain.card;

import java.math.RoundingMode;
import java.util.Objects;

/** Тут только расчёты: карточку и валюту меняет репозиторий */
public final class UpgradeRules {

    private final UpgradeBalance balance;

    public UpgradeRules(UpgradeBalance balance) {
        this.balance = Objects.requireNonNull(balance, "balance");
    }

    /** Считает статы от базы и округляет прибавку для каждого апгрейда */
    public Stats upgraded(Stats base, Boosts boosts) {
        Objects.requireNonNull(base, "base");
        Objects.requireNonNull(boosts, "boosts");
        var percentages = balance.statIncreasePercent();
        return new Stats(
                upgradedStat(base.hp(), percentages.hp(), boosts.hp()),
                upgradedStat(base.st(), percentages.st(), boosts.st()),
                upgradedStat(base.df(), percentages.df(), boosts.df()));
    }

    public int totalUpgrades(Boosts boosts) {
        Objects.requireNonNull(boosts, "boosts");
        return Math.addExact(Math.addExact(boosts.hp(), boosts.st()), boosts.df());
    }

    /** Лимит общий на карточку — хоть все апгрейды в один стат */
    public boolean upgradeAllowed(Boosts boosts) {
        return totalUpgrades(boosts) < balance.maxUpgradesPerCard();
    }

    public long goldCost(int currentCardLevel) {
        return balance.goldCost().forLevel(currentCardLevel);
    }

    public int shardCost(Rarity rarity) {
        return balance.shardCostByRarity().get(Objects.requireNonNull(rarity, "rarity"));
    }

    /** Сколько осколков прилетает за повтор карточки */
    public int shardsPerDuplicate() {
        return balance.shardsPerDuplicate();
    }

    private static int upgradedStat(int base, java.math.BigDecimal percentage, int upgrades) {
        if (base == 0 || upgrades == 0 || percentage.signum() == 0) {
            return base;
        }
        int increase = percentage.multiply(java.math.BigDecimal.valueOf(base))
                .setScale(0, RoundingMode.HALF_UP)
                .intValueExact();
        increase = Math.max(1, increase);
        return Math.addExact(base, Math.multiplyExact(increase, upgrades));
    }
}
