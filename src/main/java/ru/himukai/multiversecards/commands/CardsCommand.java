package ru.himukai.multiversecards.commands;

import ru.himukai.multiversecards.core.Command;
import ru.himukai.multiversecards.core.CommandContext;
import ru.himukai.multiversecards.core.Response;
import ru.himukai.multiversecards.domain.card.CardTemplate;
import ru.himukai.multiversecards.domain.card.Stats;
import ru.himukai.multiversecards.domain.player.OwnedCard;
import ru.himukai.multiversecards.domain.player.Player;
import ru.himukai.multiversecards.domain.player.PlayerId;
import ru.himukai.multiversecards.service.PlayerService;

import java.util.Objects;
import java.util.Optional;

public final class CardsCommand implements Command {

    private final PlayerService players;

    public CardsCommand(PlayerService players) {
        this.players = Objects.requireNonNull(players, "players");
    }

    @Override
    public String name() {
        return "cards";
    }

    @Override
    public String description() {
        return "Посмотреть свою коллекцию карточек";
    }

    @Override
    public Response execute(CommandContext ctx) {
        Optional<Player> player = players.find(playerId(ctx.userId()));
        if (player.isEmpty()) {
            return Response.text("Сначала начни игру командой /start [имя].");
        }
        if (player.get().cards().all().isEmpty()) {
            return Response.text("Коллекция пока пуста.");
        }

        StringBuilder text = new StringBuilder("Твоя коллекция (")
                .append(player.get().cards().size()).append("):\n");
        for (OwnedCard owned : player.get().cards().all()) {
            CardTemplate card = owned.template();
            Stats stats = card.baseStats();
            text.append("\n• ").append(card.name()).append(" — ").append(card.rarity())
                    .append("\n  ID: ").append(card.id().value())
                    .append("\n  HP ").append(stats.hp())
                    .append(" · ST ").append(stats.st())
                    .append(" · DF ").append(stats.df());
        }
        return Response.text(text.toString());
    }

    private static PlayerId playerId(String userId) {
        return new PlayerId(Long.parseLong(userId.substring(userId.indexOf(':') + 1)));
    }
}
