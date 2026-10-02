package ru.himukai.multiversecards.service;

import ru.himukai.multiversecards.domain.player.Player;
import ru.himukai.multiversecards.domain.player.PlayerId;

import java.util.Optional;

public interface PlayerService {
    Player start(PlayerId id, String name);

    Optional<Player> find(PlayerId id);

    void rename(PlayerId id, String name);
}