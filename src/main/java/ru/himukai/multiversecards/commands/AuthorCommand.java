package ru.himukai.multiversecards.commands;

import ru.himukai.multiversecards.core.Command;
import ru.himukai.multiversecards.core.CommandContext;
import ru.himukai.multiversecards.core.Response;

public final class AuthorCommand implements Command {

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
        return Response.text("""
                Авторы бота:
                — @hinag6shi
                — @oleacs
                """);
    }
}