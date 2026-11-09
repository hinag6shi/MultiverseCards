package ru.himukai.multiversecards.commands;

import ru.himukai.multiversecards.core.Command;
import ru.himukai.multiversecards.core.CommandContext;
import ru.himukai.multiversecards.core.Response;
import ru.himukai.multiversecards.domain.player.PlayerId;
import ru.himukai.multiversecards.service.PlayerService;

import java.util.Optional;

public final class AuthorCommand implements Command {

    private final PlayerService players;

    public AuthorCommand() {
        this.players = null;
    }

    public AuthorCommand(PlayerService players) {
        this.players = players;
    }

    @Override
    public String name() {
        return "author";
    }

    @Override
    public String description() {
        return "Информация об авторах бота";
    }

    @Override
    public Response execute(CommandContext ctx) {
        String greeting = playerName(ctx.userId()).map(name -> "Привет, " + name + "! 😊\n\n").orElse("");
        return Response.text(greeting + """
                Авторы бота:
                — @hinag6shi
                — @oleacs
                """);
    }

    private Optional<String> playerName(String userId) {
        if (players == null) return Optional.empty();
        return players.find(new PlayerId(Long.parseLong(userId.substring(userId.indexOf(':') + 1))))
                .map(player -> player.name());
    }
}
