package ru.himukai.multiversecards.commands;

import ru.himukai.multiversecards.core.Command;
import ru.himukai.multiversecards.core.CommandContext;
import ru.himukai.multiversecards.core.Response;
import ru.himukai.multiversecards.domain.player.PlayerId;
import ru.himukai.multiversecards.service.PlayerService;

import java.util.Objects;

public final class NameCommand implements Command {

    private final PlayerService players;

    public NameCommand(PlayerService players) {
        this.players = Objects.requireNonNull(players, "players");
    }

    @Override
    public String name() {
        return "name";
    }

    @Override
    public String usage() {
        return "/name <новое имя>";
    }

    @Override
    public String description() {
        return "Изменить имя, которым бот к тебе обращается";
    }

    @Override
    public Response execute(CommandContext ctx) {
        PlayerId id = new PlayerId(Long.parseLong(ctx.userId().substring(ctx.userId().indexOf(':') + 1)));
        if (ctx.args().isEmpty()) {
            return Response.text("Укажи новое имя: " + usage());
        }
        String name = String.join(" ", ctx.args()).strip();
        return players.rename(id, name)
                .map(player -> Response.text("Хорошо, " + player.name() + "! Буду так к тебе обращаться. 😊"))
                .orElseGet(() -> Response.text("Сначала начни игру командой /start."));
    }
}
