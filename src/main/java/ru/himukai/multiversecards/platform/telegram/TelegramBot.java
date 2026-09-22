package ru.himukai.multiversecards.platform.telegram;

import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.himukai.multiversecards.platform.telegram.core.CommandDispatcher;

public final class TelegramBot implements LongPollingSingleThreadUpdateConsumer {

    private static final Logger log = LoggerFactory.getLogger(TelegramBot.class);

    private final CommandDispatcher dispatcher;
    private final TelegramClient client;

    public TelegramBot(
            String botToken,
            CommandDispatcher dispatcher,
            OkHttpClient httpClient
    ) {
        this.dispatcher = dispatcher;
        this.client = new OkHttpTelegramClient(httpClient, botToken);
    }

    @Override
    public void consume(Update update) {
        log.info("Получено обновление: {}", update);

        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        String chatId = update.getMessage().getChatId().toString();
        String text = update.getMessage().getText();

        String responseText = dispatcher.handle(text);

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(responseText)
                .build();

        try {
            client.execute(message);
        } catch (Exception e) {
            log.error("Не удалось отправить ответ в чат {}", chatId, e);
        }
    }
}
