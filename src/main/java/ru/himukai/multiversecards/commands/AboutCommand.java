package ru.himukai.multiversecards.commands;

import ru.himukai.multiversecards.core.Command;
import ru.himukai.multiversecards.core.CommandContext;
import ru.himukai.multiversecards.core.Response;

public final class AboutCommand implements Command {

    @Override
    public String name() {
        return "about";
    }

    @Override
    public String description() {
        return "информация о назначении бота";
    }

    @Override
    public Response execute(CommandContext ctx) {
        return Response.text("""
                Multiverse Cards — это коллекционная игра, \
                где игрокам предстоит соревноваться с помощью карточек \
                персонажей и существ из разных вселенных.

                Вы развиваете свою коллекцию, получаете стартовый набор ресурсов, \
                усиливаете карточки и взаимодействуете с другими игроками!
                Покорите все вселенные!

                Желаем вам удачи в развитии!""");
    }
}