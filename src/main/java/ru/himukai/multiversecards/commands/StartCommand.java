package ru.himukai.multiversecards.commands;

import ru.himukai.multiversecards.core.Command;
import ru.himukai.multiversecards.core.CommandContext;
import ru.himukai.multiversecards.core.Response;
import ru.himukai.multiversecards.domain.player.PlayerId;
import ru.himukai.multiversecards.domain.player.Player;
import ru.himukai.multiversecards.service.PlayerService;

import java.util.Objects;

public final class StartCommand implements Command {

    private final PlayerService players;

    public StartCommand(PlayerService players) {
        this.players = Objects.requireNonNull(players, "players");
    }

    @Override
    public String usage() {
        return "/start";
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
        PlayerId id = playerId(ctx.userId());
        var existing = players.find(id);
        if (existing.isPresent()) {
            Player player = existing.get();
            boolean setWasClaimed = player.starterSetClaimed();
            Player refreshed = players.start(id, player.name());
            if (setWasClaimed) {
                return Response.text("С возвращением, " + refreshed.name()
                        + "! Стартовый набор уже выдавался и повторно не выдаётся.");
            }
            return Response.text("С возвращением, " + refreshed.name()
                    + "! Стартовый набор выдан — он выдаётся только один раз.");
        }
        if (!ctx.args().isEmpty()) {
            String name = String.join(" ", ctx.args()).strip();
            Player registered = players.start(id, name);
            return Response.text("Приятно познакомиться, " + registered.name()
                    + "! Стартовый набор выдан. Посмотреть: /cards");
        }
        return Response.text("Сначала напиши /start, затем отправь имя в течение 30 секунд.");
    }

    /** "tg:12345" -> PlayerId(12345) */
    private static PlayerId playerId(String userId) {
        return new PlayerId(Long.parseLong(userId.substring(userId.indexOf(':') + 1)));
    }
}
