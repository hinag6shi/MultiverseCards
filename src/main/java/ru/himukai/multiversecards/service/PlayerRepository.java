package ru.himukai.multiversecards.service;

import ru.himukai.multiversecards.domain.player.Player;
import ru.himukai.multiversecards.domain.player.PlayerId;
import ru.himukai.multiversecards.domain.player.OwnedCard;

import java.util.Collection;
import java.util.Optional;

public interface PlayerRepository {
    Optional<Player> find(PlayerId id);

    void save(Player player);

    boolean grantStarterSetOnce(PlayerId id, Collection<OwnedCard> cards);
}
