package ru.himukai.multiversecards.platform.telegram.core;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Optional;
import java.util.TreeMap;
import java.util.Map;

public final class CommandRegistry {

    private final Map<String, Command> commands = new TreeMap<>();

    public CommandRegistry register(Command... newCommands) {
        for (Command command : newCommands) {
            String key = normalize(command.name());
            if (commands.putIfAbsent(key, command) != null) {
                throw new IllegalArgumentException("Команда уже зарегистрирована: " + key);
            }
        }
        return this;
    }

    public Optional<Command> find(String name) {
        return Optional.ofNullable(commands.get(normalize(name)));
    }

    public Collection<Command> all() {
        return Collections.unmodifiableCollection(commands.values());
    }

    private static String normalize(String name) {
        String n = name.strip().toLowerCase(Locale.ROOT);
        return n.startsWith("/") ? n.substring(1) : n;
    }
}