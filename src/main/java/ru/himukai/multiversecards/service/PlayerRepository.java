package ru.himukai.multiversecards.service;

import ru.himukai.multiversecards.domain.player.Player;
import ru.himukai.multiversecards.domain.player.PlayerId;
import ru.himukai.multiversecards.domain.player.OwnedCard;
import ru.himukai.multiversecards.domain.card.TemplateId;
import ru.himukai.multiversecards.domain.card.UpgradePayment;
import ru.himukai.multiversecards.domain.card.UpgradeStat;

import java.util.Collection;
import java.util.Optional;

public interface PlayerRepository {
    Optional<Player> find(PlayerId id);

    void save(Player player);

    boolean grantStarterSetOnce(PlayerId id, Collection<OwnedCard> cards);

    boolean upgradeCard(PlayerId playerId, TemplateId cardId, UpgradeStat stat, UpgradePayment payment,
                        int expectedLevel, int maxLevel, long goldCost, int shardCost);

    boolean convertDuplicateToShards(PlayerId playerId, TemplateId cardId, int shardAmount);
}
