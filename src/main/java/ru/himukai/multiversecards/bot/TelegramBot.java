package ru.himukai.multiversecards.bot;

import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.himukai.multiversecards.core.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class TelegramBot implements LongPollingSingleThreadUpdateConsumer {

    private static final Logger log = LoggerFactory.getLogger(TelegramBot.class);

    private final CommandDispatcher dispatcher;
    private final TelegramClient client;
    private final Map<String, Integer> lastMessageId = new ConcurrentHashMap<>();

    public TelegramBot(String botToken, CommandDispatcher dispatcher, OkHttpClient httpClient) {
        this.dispatcher = dispatcher;
        this.client = new OkHttpTelegramClient(httpClient, botToken);
    }

    @Override
    public void consume(Update update) {
        if (update.hasCallbackQuery()) {
            var cq = update.getCallbackQuery();
            String chatId = cq.getMessage().getChatId().toString();
            answerCallback(cq.getId());
            handle(chatId, cq.getData());
        } else if (update.hasMessage() && update.getMessage().hasText()) {
            handle(update.getMessage().getChatId().toString(), update.getMessage().getText());
        }
    }

    private void handle(String chatId, String text) {
        send(chatId, dispatcher.handle("tg:" + chatId, text));
    }

    private void send(String chatId, Response response) {
        if (!(response instanceof Response.Text(String text, Button.Keyboard keyboard, Response.RenderMode mode))) {
            return;
        }

        InlineKeyboardMarkup markup = toKeyboard(keyboard);

        if (mode == Response.RenderMode.UPDATE && lastMessageId.containsKey(chatId)) {
            try {
                client.execute(EditMessageText.builder()
                        .chatId(chatId).messageId(lastMessageId.get(chatId))
                        .text(text).replyMarkup(markup).build());
                return;
            } catch (Exception e) {
                log.warn("Не удалось отредактировать сообщение в чате {}, отправляю новое", chatId, e);
            }
        }

        try {
            var sent = client.execute(SendMessage.builder()
                    .chatId(chatId).text(text).replyMarkup(markup).build());
            lastMessageId.put(chatId, sent.getMessageId());
        } catch (Exception e) {
            log.error("Не удалось отправить ответ в чат {}", chatId, e);
        }
    }

    private InlineKeyboardMarkup toKeyboard(Button.Keyboard keyboard) {
        if (keyboard.isEmpty()) {
            return null;
        }
        var rows = keyboard.rows().stream()
                .map(row -> new InlineKeyboardRow(row.stream()
                        .map(b -> InlineKeyboardButton.builder().text(b.label()).callbackData(b.commandText()).build())
                        .toList()))
                .toList();
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    private void answerCallback(String callbackQueryId) {
        try {
            client.execute(AnswerCallbackQuery.builder().callbackQueryId(callbackQueryId).build());
        } catch (Exception e) {
            log.warn("Не удалось ответить на нажатие кнопки {}", callbackQueryId, e);
        }
    }
}