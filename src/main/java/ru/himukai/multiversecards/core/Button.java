package ru.himukai.multiversecards.core;

import java.util.Objects;

/**
 * То же значение, что лежит в {@code commandText}
 */
public record Button(String label, String commandText) {

    public Button {
        requireNotBlank(label, "label");
        requireNotBlank(commandText, "commandText");
    }

    /**
     * Собирает команду из имени и аргументов
     * Например, {@code command("about", "help", "author")} даст "/help author"
     */
    public static Button command(String label, String commandName, String... args) {
        StringBuilder text = new StringBuilder("/").append(commandName);
        for (String arg : args) {
            text.append(' ').append(arg);
        }
        return new Button(label, text.toString());
    }

    private static void requireNotBlank(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " не может быть пустым");
        }
    }
}
