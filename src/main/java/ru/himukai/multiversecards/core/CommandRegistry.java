package ru.himukai.multiversecards.core;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public final class CommandRegistry {

    private final Map<String, Command> commands = new TreeMap<>();

    public CommandRegistry register(Command... newCommands) {
        for (Command command : newCommands) {
            String key = normalize(command.name());
            if (key.isEmpty()) {
                throw new IllegalArgumentException("Имя команды не может быть пустым");
            }
            if (commands.putIfAbsent(key, command) != null) {
                throw new IllegalArgumentException("Команда уже зарегистрирована: " + key);
            }
        }
        return this;
    }

    public Optional<Command> find(String name) {
        return Optional.ofNullable(commands.get(normalize(name)));
    }

    /** Алфавитный порядок */
    public Collection<Command> all() {
        return Collections.unmodifiableCollection(commands.values());
    }

    private static String normalize(String name) {
        String n = name.strip().toLowerCase(Locale.ROOT);
        return n.startsWith("/") ? n.substring(1) : n;
    }
}