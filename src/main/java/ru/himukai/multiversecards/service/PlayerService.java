package ru.himukai.multiversecards.service;

import ru.himukai.multiversecards.domain.player.Player;
import ru.himukai.multiversecards.domain.player.PlayerId;

public interface PlayerService {
    Player start(PlayerId id, String name);
}