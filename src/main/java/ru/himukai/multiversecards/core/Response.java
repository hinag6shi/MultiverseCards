package ru.himukai.multiversecards.core;

import java.util.Objects;

/**
 * @param text       текст сообщения
 * @param keyboard   кнопки под сообщением (может быть {@link Keyboard#NONE})
 * @param renderMode {@link RenderMode#NEW} — отправить новое сообщение;
 *                   {@link RenderMode#UPDATE} — заменить сообщение, с кнопки которого пришла команда
 *                   (если команду набрали вручную, платформа отправит новое сообщение)
 */
public record Response(String text, Keyboard keyboard, RenderMode renderMode) {

    public enum RenderMode { NEW, UPDATE }

    public Response {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(renderMode, "renderMode");
        keyboard = keyboard == null ? Keyboard.NONE : keyboard;
    }

    public static Response text(String text) {
        return new Response(text, Keyboard.NONE, RenderMode.NEW);
    }

    public static Response text(String text, Keyboard keyboard) {
        return new Response(text, keyboard, RenderMode.NEW);
    }

    public static Response update(String text, Keyboard keyboard) {
        return new Response(text, keyboard, RenderMode.UPDATE);
    }
}