package ru.himukai.multiversecards.core;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class CommandDispatcher {

    private final CommandRegistry registry;

    public CommandDispatcher(CommandRegistry registry) {
        this.registry = registry;
    }

    /**
     * @param userId идентификатор пользователя/чата
     * @param text   текст сообщения или commandText нажатой кнопки,
     *               например "/help ping" или "/help@MyBot ping"
     */
    public Response handle(String userId, String text) {
        if (text == null || text.isBlank()) {
            return Response.text("Пустое сообщение. Список команд: /help");
        }

        String trimmed = text.strip();
        if (!trimmed.startsWith("/")) {
            return Response.text("Команды начинаются со слэша, например /help");
        }

        String[] parts = trimmed.split("\\s+");
        String rawName = parts[0].substring(1);

        int at = rawName.indexOf('@');
        String name = at >= 0 ? rawName.substring(0, at) : rawName;

        if (name.isEmpty()) {
            return Response.text("Не указана команда. Список команд: /help");
        }

        List<String> args = Arrays.asList(parts).subList(1, parts.length);

        Optional<Command> command = registry.find(name);
        if (command.isEmpty()) {
            return Response.text("Команда «/" + name + "» не найдена. Список команд: /help");
        }

        return command.get().execute(new CommandContext(userId, args));
    }
}