package ru.himukai.multiversecards.service;

import ru.himukai.multiversecards.domain.player.CardCollection;
import ru.himukai.multiversecards.domain.player.Player;
import ru.himukai.multiversecards.domain.player.PlayerId;

import java.util.Objects;

public final class DefaultPlayerService implements PlayerService {

    private final PlayerRepository repository;

    public DefaultPlayerService(PlayerRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    /** Создаёт игрока или, если он уже есть, меняет ему имя. Кошелёк и карточки сохраняются. */
    @Override
    public Player start(PlayerId id, String name) {
        Player player = repository.find(id).orElse(null);
        if (player == null) {
            player = new Player(id, name, 0, new CardCollection());
        } else {
            player.rename(name);
        }
        repository.save(player);
        return player;
    }
}
