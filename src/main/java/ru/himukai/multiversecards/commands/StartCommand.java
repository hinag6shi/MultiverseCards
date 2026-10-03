package ru.himukai.multiversecards.commands;

import ru.himukai.multiversecards.core.Command;
import ru.himukai.multiversecards.core.CommandContext;
import ru.himukai.multiversecards.core.Response;

public final class StartCommand implements Command {

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
        return Response.text("Приятно познакомиться, " + name + "! ✨");
    }
}
