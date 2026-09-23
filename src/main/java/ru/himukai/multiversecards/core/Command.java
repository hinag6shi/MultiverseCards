package ru.himukai.multiversecards.core;

public interface Command {
    String name();

    String description();

    default String usage() {
        return "/" + name();
    }

    Response execute(CommandContext ctx);
}