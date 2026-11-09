package ru.himukai.multiversecards.service;

import ru.himukai.multiversecards.domain.player.CardCollection;
import ru.himukai.multiversecards.domain.player.OwnedCard;
import ru.himukai.multiversecards.domain.player.Player;
import ru.himukai.multiversecards.domain.player.PlayerId;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class DefaultPlayerService implements PlayerService {

    private final PlayerRepository repository;
    private final CardCatalog catalog;

    public DefaultPlayerService(PlayerRepository repository) {
        this(repository, new DefaultCardCatalog(List.of()));
    }

    public DefaultPlayerService(PlayerRepository repository, CardCatalog catalog) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.catalog = Objects.requireNonNull(catalog, "catalog");
    }

    /** Создаёт игрока или, если он уже есть, меняет ему имя. Кошелёк и карточки сохраняются. */
    @Override
    public Player start(PlayerId id, String name) {
        Player player = repository.find(id).orElse(null);
        if (player == null) {
            player = new Player(id, name, 0, starterCollection());
        } else {
            player.rename(name);
            // Мигрируем старые профили, созданные до подключения стартовых карточек.
            if (player.cards().size() == 0) {
                for (OwnedCard card : starterCollection().all()) {
                    player.cards().add(card);
                }
            }
        }
        repository.save(player);
        return player;
    }

    private CardCollection starterCollection() {
        Instant obtainedAt = Instant.now();
        List<OwnedCard> starterCards = catalog.starterSet().stream()
                .map(template -> OwnedCard.fresh(template, obtainedAt))
                .toList();
        return new CardCollection(starterCards);
    }

    @Override
    public Optional<Player> find(PlayerId id) {
        return repository.find(Objects.requireNonNull(id, "id"));
    }
}
