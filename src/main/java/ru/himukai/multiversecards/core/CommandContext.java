package ru.himukai.multiversecards.core;

import java.util.List;

/**
 * @param userId идентификатор пользователя с префиксом платформы, p.s. "tg:12345"
 * @param args   аргументы команды (без имени), неизменяемый список
 */
public record CommandContext(String userId, List<String> args) {

    public CommandContext {
        args = List.copyOf(args);
    }
}
