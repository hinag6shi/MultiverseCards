package ru.himukai.multiversecards.core;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class CommandDispatcher {

    private final CommandRegistry registry;

    public CommandDispatcher(CommandRegistry registry) {
        this.registry = registry;
    }

    public Response handle(String userId, String text) {
        if (text == null || text.isBlank()) {
            return Response.text("Пустое сообщение. Список команд: /help");
        }
        if (!text.strip().startsWith("/")) {
            return Response.text("Команды начинаются со слэша, например /help");
        }

        String[] parts = text.strip().split("\\s+");
        String name = parts[0].substring(1).split("@", 2)[0];
        if (name.isEmpty()) {
            return Response.text("Не указана команда. Список команд: /help");
        }

        Optional<Command> command = registry.find(name);
        if (command.isEmpty()) {
            return Response.text("Команда «/" + name + "» не найдена. Список команд: /help");
        }

        List<String> args = Arrays.asList(parts).subList(1, parts.length);
        return command.get().execute(new CommandContext(userId, args));
    }
}