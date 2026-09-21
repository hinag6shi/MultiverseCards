package ru.himukai.platform.telegram.commands;

import ru.himukai.platform.telegram.core.Command;
import ru.himukai.platform.telegram.core.CommandRegistry;

import java.util.List;

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
    public String execute(List<String> args) {
        if (args.isEmpty()) {
            return allCommands();
        }
        String requested = args.getFirst();
        return registry.find(requested)
                .map(HelpCommand::details)
                .orElse("Команда «" + requested + "» не найдена. Список команд: /help");
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