package ru.himukai.multiversecards.core;

import java.util.function.Function;

public final class StubCommand implements Command {

    private final String name;
    private final Function<CommandContext, Response> handler;
    private CommandContext lastContext;

    public StubCommand(String name) {
        this(name, ctx -> Response.text("ok"));
    }

    public StubCommand(String name, Function<CommandContext, Response> handler) {
        this.name = name;
        this.handler = handler;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return "описание " + name;
    }

    @Override
    public Response execute(CommandContext ctx) {
        lastContext = ctx;
        return handler.apply(ctx);
    }

    public CommandContext lastContext() {
        return lastContext;
    }
}
