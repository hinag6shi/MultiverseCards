package ru.himukai.multiversecards.commands;

import ru.himukai.multiversecards.core.Command;
import ru.himukai.multiversecards.core.CommandContext;
import ru.himukai.multiversecards.core.Response;
import ru.himukai.multiversecards.domain.card.CardTemplate;
import ru.himukai.multiversecards.domain.card.Stats;
import ru.himukai.multiversecards.domain.card.TemplateId;
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
        return "Посмотреть коллекцию или открыть карточку по ID";
    }

    @Override
    public String usage() {
        return "/cards [id]";
    }

    @Override
    public Response execute(CommandContext ctx) {
        Optional<Player> player = players.find(playerId(ctx.userId()));
        if (player.isEmpty()) {
            return Response.text("Сначала начни игру командой /start [имя].");
        }
        if (ctx.args().size() > 1) {
            return Response.text("Формат: /cards [id]");
        }
        if (!ctx.args().isEmpty()) {
            String id = ctx.args().get(0);
            Optional<OwnedCard> owned = player.get().cards().find(new TemplateId(id));
            if (owned.isEmpty()) {
                return Response.text("Карточка с ID «" + id + "» не найдена в твоей коллекции.");
            }
            CardTemplate card = owned.get().template();
            String caption = "✨ " + card.name() + "\n" + card.universe() + " · " + card.rarity()
                    + "\n🆔 " + card.id().value() + "\n" + statsLine(card.baseStats());
            return Response.photo(caption, card.imageResource());
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
                    .append("\n  ").append(statsLine(stats));
        }
        return Response.text(text.toString());
    }

    private static String statsLine(Stats stats) {
        return "❤️ " + stats.hp() + "   ⚔️ " + stats.st() + "   🛡️ " + stats.df();
    }

    private static PlayerId playerId(String userId) {
        return new PlayerId(Long.parseLong(userId.substring(userId.indexOf(':') + 1)));
    }
}
