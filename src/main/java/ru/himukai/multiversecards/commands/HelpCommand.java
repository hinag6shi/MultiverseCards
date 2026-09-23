package ru.himukai.multiversecards.commands;

import ru.himukai.multiversecards.core.Command;
import ru.himukai.multiversecards.core.CommandContext;
import ru.himukai.multiversecards.core.CommandRegistry;
import ru.himukai.multiversecards.core.Response;

public final class HelpCommand implements Command {

    private final CommandRegistry registry;

    public HelpCommand(CommandRegistry registry) {
        this.registry = registry;
    }

    @Override
    public String name() {
        return "help";
    }

    @Override
    public String description() {
        return "Список команд или справка по одной команде";
    }

    @Override
    public String usage() {
        return "/help [команда]";
    }

    @Override
    public Response execute(CommandContext ctx) {
        if (ctx.args().isEmpty()) {
            return Response.text(allCommands());
        }
        String requested = ctx.args().getFirst();
        return registry.find(requested)
                .map(command -> Response.text(details(command)))
                .orElse(Response.text("Команда «" + requested + "» не найдена. Список команд: /help"));
    }

    private String allCommands() {
        StringBuilder sb = new StringBuilder("Доступные команды:\n");
        for (Command command : registry.all()) {
            sb.append('/').append(command.name())
                    .append(" — ").append(command.description()).append('\n');
        }
        sb.append("\nПодробнее: /help <команда>");
        return sb.toString();
    }

    private static String details(Command command) {
        return command.usage() + "\n" + command.description();
    }
}