package ru.himukai.multiversecards.domain.card;

/** Правила усиления карточек: прирост характеристик и цена в осколках. Чистые формулы. */
public final class UpgradeRules {

    private static final double HP_PERCENT = 0.025;
    private static final double ST_PERCENT = 0.035;
    private static final double DF_PERCENT = 0.03;

    /** Сколько усилений карточки можно сделать за осколки (всего, на все характеристики). */
    public static final int MAX_SHARD_UPGRADES = 21;

    private UpgradeRules() {
    }

    /** Характеристики с учётом усилений: считаем от базы, округляем один раз. */
    public static Stats upgraded(Stats base, Boosts boosts) {
        return new Stats(
                scale(base.hp(), HP_PERCENT, boosts.hp()),
                scale(base.st(), ST_PERCENT, boosts.st()),
                scale(base.df(), DF_PERCENT, boosts.df()));
    }

    /** Сколько усилений уже сделано всего. */
    public static int totalUpgrades(Boosts boosts) {
        return boosts.hp() + boosts.st() + boosts.df();
    }

    /** Можно ли сделать ещё одно усиление за осколки. */
    public static boolean shardUpgradeAllowed(Boosts boosts) {
        return totalUpgrades(boosts) < MAX_SHARD_UPGRADES;
    }

    /** Осколков за одно усиление; зависит только от редкости. */
    public static int shardCost(Rarity rarity) {
        return switch (rarity) {
            case COMMON -> 20;
            case UNCOMMON -> 17;
            case RARE -> 14;
            case EPIC -> 11;
            case MYTHIC -> 8;
            case LEGENDARY -> 5;
            case DIVINE -> 3;
        };
    }

    private static int scale(int base, double percent, int upgrades) {
        return (int) Math.round(base * (1 + percent * upgrades));
    }
}