package ru.himukai.multiversecards.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class CommandDispatcher {

    private static final Logger log = LoggerFactory.getLogger(CommandDispatcher.class);

    private static final String HELP_HINT = "Список команд: /help";

    private final CommandRegistry registry;

    public CommandDispatcher(CommandRegistry registry) {
        this.registry = registry;
    }

    public Response handle(String userId, String text) {
        if (text == null || text.isBlank()) {
            return Response.text("Пустое сообщение. " + HELP_HINT);
        }

        String[] parts = text.strip().split("\\s+");
        if (!parts[0].startsWith("/")) {
            return Response.text("Команды начинаются со слэша, например /help");
        }

        String name = commandName(parts[0]);
        if (name.isEmpty()) {
            return Response.text("Не указана команда. " + HELP_HINT);
        }

        Optional<Command> command = registry.find(name);
        if (command.isEmpty()) {
            return Response.text("Команда «/" + name + "» не найдена. " + HELP_HINT);
        }

        List<String> args = Arrays.asList(parts).subList(1, parts.length);
        return execute(command.get(), new CommandContext(userId, args));
    }

    private static String commandName(String token) {
        return token.substring(1).split("@", 2)[0];
    }

    private Response execute(Command command, CommandContext ctx) {
        try {
            return command.execute(ctx);
        } catch (RuntimeException e) {
            log.error("Ошибка при выполнении /{} для {}", command.name(), ctx.userId(), e);
            return Response.text("Не удалось выполнить команду. Попробуйте позже.");
        }
    }
}
