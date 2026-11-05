package ru.himukai.multiversecards.commands;

import ru.himukai.multiversecards.core.Command;
import ru.himukai.multiversecards.core.CommandContext;
import ru.himukai.multiversecards.core.Response;
import ru.himukai.multiversecards.domain.player.PlayerId;
import ru.himukai.multiversecards.service.PlayerService;

import java.util.Objects;

public final class StartCommand implements Command {

    private final PlayerService players;

    public StartCommand(PlayerService players) {
        this.players = Objects.requireNonNull(players, "players");
    }

    @Override
    public String usage() {
        return "start";
    }

    @Override
    public String description() {
        return "Начать игру и познакомиться с ботом";
    }

    @Override
    public String name() {
        return "start";
    }

    @Override
    public Response execute(CommandContext ctx) {
        if (ctx.args().isEmpty()) {
            return Response.text("""
                    Привет! 😊
                    Добро пожаловать в Multiverse Cards 🃏\n
                    Как мне к тебе обращаться? Напиши /start [своё имя],
                    например: /start ПроИгрок
                    """);
        }

        String name = String.join(" ", ctx.args());
        players.start(playerId(ctx.userId()), name);
        return Response.text("Приятно познакомиться, " + name + "! ✨");
    }

    /** "tg:12345" -> PlayerId(12345) */
    private static PlayerId playerId(String userId) {
        return new PlayerId(Long.parseLong(userId.substring(userId.indexOf(':') + 1)));
    }
}
