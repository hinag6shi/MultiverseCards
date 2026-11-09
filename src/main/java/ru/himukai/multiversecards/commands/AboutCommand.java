package ru.himukai.multiversecards.commands;

import ru.himukai.multiversecards.core.Command;
import ru.himukai.multiversecards.core.CommandContext;
import ru.himukai.multiversecards.core.Response;
import ru.himukai.multiversecards.domain.player.PlayerId;
import ru.himukai.multiversecards.service.PlayerService;

import java.util.Optional;

public final class AboutCommand implements Command {

    private final PlayerService players;

    public AboutCommand() {
        this.players = null;
    }

    public AboutCommand(PlayerService players) {
        this.players = players;
    }

    @Override
    public String name() {
        return "about";
    }

    @Override
    public String description() {
        return "Информация о назначении бота";
    }

    @Override
    public Response execute(CommandContext ctx) {
        String greeting = playerName(ctx.userId()).map(name -> "Привет, " + name + "! 😊\n\n").orElse("");
        return Response.text(greeting + """
                Multiverse Cards — это коллекционная игра 🧩, \
                где игрокам предстоит соревноваться с помощью карточек 🃏 \
                персонажей и существ из разных вселенных 💫

                Вы развиваете свою коллекцию, получаете стартовый набор ресурсов 💰, \
                усиливаете карточки и взаимодействуете с другими игроками! ⚔️
                Покорите все вселенные! 🌠

                Желаем вам удачи в развитии! 🍀""");
    }

    private Optional<String> playerName(String userId) {
        if (players == null) return Optional.empty();
        return players.find(new PlayerId(Long.parseLong(userId.substring(userId.indexOf(':') + 1))))
                .map(player -> player.name());
    }
}
