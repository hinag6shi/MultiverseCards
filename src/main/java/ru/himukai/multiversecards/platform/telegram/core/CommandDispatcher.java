package ru.himukai.multiversecards.platform.telegram.core;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class CommandDispatcher {

    private final CommandRegistry registry;

    public CommandDispatcher(CommandRegistry registry) {
        this.registry = registry;
    }

    public String handle(String text) {
        if (text == null || text.isBlank()) {
            return "Пустое сообщение. Список команд: /help";
        }

        String trimmed = text.strip();
        if (!trimmed.startsWith("/")) {
            return "Команды начинаются со слэша, например /help";
        }

        String[] parts = trimmed.split("\\s+");
        String rawName = parts[0].substring(1); // отрезаем "/"

        // Для групп, может прийти "/help@ИмяБота"
        int at = rawName.indexOf('@');
        String name = at >= 0 ? rawName.substring(0, at) : rawName;

        if (name.isEmpty()) {
            return "Не указана команда. Список команд: /help";
        }

        List<String> args = Arrays.asList(parts).subList(1, parts.length);

        Optional<Command> command = registry.find(name);
        if (command.isEmpty()) {
            return "Команда «/" + name + "» не найдена. Список команд: /help";
        }

        return command.get().execute(args);
    }
}