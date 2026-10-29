package ru.himukai.multiversecards.service;

import ru.himukai.multiversecards.domain.player.Player;
import ru.himukai.multiversecards.domain.player.PlayerId;

import java.util.Optional;

public interface PlayerRepository {
    Optional<Player> find(PlayerId id);

    void save(Player player);
}