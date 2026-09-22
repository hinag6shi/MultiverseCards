package ru.himukai.multiversecards.platform.telegram.commands;

import ru.himukai.multiversecards.platform.telegram.core.Command;

import java.util.List;

public final class AuthorCommand implements Command {

    @Override
    public String name() {
        return "author";
    }

    @Override
    public String description() {
        return "информация об авторах бота";
    }

    @Override
    public String execute(List<String> args) {
        return """
                Authors:
                — hinag6shi
                — 0leacs
                """;
    }
}