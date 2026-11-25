package ru.himukai.multiversecards.core;

import java.util.List;

/**
 * @param userId ID пользователя с префиксом платформы, например "tg:12345"
 * @param args   аргументы после имени команды; список нельзя менять
 */
public record CommandContext(String userId, List<String> args) {

    public CommandContext {
        args = List.copyOf(args);
    }
}
