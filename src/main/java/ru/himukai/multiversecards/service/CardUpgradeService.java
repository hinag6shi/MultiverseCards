package ru.himukai.multiversecards.service;

import ru.himukai.multiversecards.domain.card.TemplateId;
import ru.himukai.multiversecards.domain.card.UpgradeBalance;
import ru.himukai.multiversecards.domain.card.UpgradePayment;
import ru.himukai.multiversecards.domain.card.UpgradeRules;
import ru.himukai.multiversecards.domain.card.UpgradeStat;
import ru.himukai.multiversecards.domain.player.PlayerId;

import java.util.Objects;

/** Считает цену апгрейда и просит репозиторий сохранить всё одним заходом */
public final class CardUpgradeService {

    private final PlayerRepository repository;
    private final UpgradeBalance balance;
    private final UpgradeRules rules;

    public CardUpgradeService(PlayerRepository repository, UpgradeBalance balance) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.balance = Objects.requireNonNull(balance, "balance");
        this.rules = new UpgradeRules(balance);
    }

    /** Вернёт false, если карточки нет, денег не хватает или лимит уже выбран */
    public boolean upgrade(PlayerId playerId, TemplateId cardId, UpgradeStat stat, UpgradePayment payment) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(cardId, "cardId");
        Objects.requireNonNull(stat, "stat");
        Objects.requireNonNull(payment, "payment");

        var owned = repository.find(playerId).flatMap(player -> player.cards().find(cardId));
        if (owned.isEmpty()) {
            return false;
        }
        int level = owned.get().upgradeLevel();
        if (!rules.upgradeAllowed(owned.get().boosts())) {
            return false;
        }

        long goldCost = payment == UpgradePayment.GOLD ? rules.goldCost(level) : 0;
        int shardCost = payment == UpgradePayment.SHARDS
                ? rules.shardCost(owned.get().template().rarity()) : 0;
        return repository.upgradeCard(playerId, cardId, stat, payment, level,
                balance.maxUpgradesPerCard(), goldCost, shardCost);
    }

    /** Зови это, когда игроку снова выпала карточка, которая у него уже есть */
    public boolean convertDuplicateToShards(PlayerId playerId, TemplateId cardId) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(cardId, "cardId");
        if (repository.find(playerId).flatMap(player -> player.cards().find(cardId)).isEmpty()) {
            return false;
        }
        return repository.convertDuplicateToShards(playerId, cardId, balance.shardsPerDuplicate());
    }
}
