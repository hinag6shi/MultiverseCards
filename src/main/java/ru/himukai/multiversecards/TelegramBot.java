package ru.himukai.multiversecards.bot;

import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.himukai.multiversecards.core.Button;
import ru.himukai.multiversecards.core.CommandDispatcher;
import ru.himukai.multiversecards.core.Response;

import java.util.List;

/**
 * Единственный класс, который знает про Telegram: принимает Update,
 * достаёт из него текст (обычного сообщения или нажатой кнопки),
 * отдаёт его в CommandDispatcher и отправляет обратно ответ,
 * при необходимости с inline-клавиатурой.
 */
public final class TelegramBot implements LongPollingSingleThreadUpdateConsumer {

    private static final Logger log = LoggerFactory.getLogger(TelegramBot.class);

    private final CommandDispatcher dispatcher;
    private final TelegramClient client;

    /**
     * @param botToken   токен бота от @BotFather
     * @param dispatcher диспетчер команд
     * @param httpClient настроенный OkHttpClient (с прокси или без —
     *                   собирается в Main), используется для запросов к Telegram
     */
    public TelegramBot(String botToken, CommandDispatcher dispatcher, OkHttpClient httpClient) {
        this.dispatcher = dispatcher;
        // ВНИМАНИЕ: сигнатура конструктора OkHttpTelegramClient(httpClient, token)
        // не проверена компиляцией — сверьте с вашей версией telegrambots-client.
        this.client = new OkHttpTelegramClient(httpClient, botToken);
    }

    @Override
    public void consume(Update update) {
        if (update.hasCallbackQuery()) {
            handleButtonClick(update.getCallbackQuery());
            return;
        }
        if (update.hasMessage() && update.getMessage().hasText()) {
            String chatId = update.getMessage().getChatId().toString();
            handle(chatId, update.getMessage().getText());
        }
    }

    private void handleButtonClick(CallbackQuery callbackQuery) {
        String chatId = callbackQuery.getMessage().getChatId().toString();
        // callbackData — это commandText из Button, обрабатывается как обычный текст
        String callbackData = callbackQuery.getData();

        // Убираем "часики" на кнопке в интерфейсе пользователя
        answerCallback(callbackQuery.getId());

        handle(chatId, callbackData);
    }

    private void handle(String chatId, String text) {
        Response response = dispatcher.handle("tg:" + chatId, text);
        send(chatId, response);
    }

    private void send(String chatId, Response response) {
        if (response instanceof Response.Text(String text1, List<Button> buttons)) {
            SendMessage message = SendMessage.builder()
                    .chatId(chatId)
                    .text(text1)
                    .replyMarkup(toKeyboard(buttons))
                    .build();
            execute(chatId, message);
        }
    }

    private InlineKeyboardMarkup toKeyboard(List<Button> buttons) {
        if (buttons.isEmpty()) {
            return null; // без клавиатуры
        }
        // Простой вариант: одна кнопка в ряд. Чтобы сделать сетку,
        // сгруппируйте несколько Button в один InlineKeyboardRow.
        List<InlineKeyboardRow> rows = buttons.stream()
                .map(button -> new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text(button.label())
                                .callbackData(button.commandText())
                                .build()))
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

    private void execute(String chatId, SendMessage message) {
        try {
            client.execute(message);
        } catch (Exception e) {
            log.error("Не удалось отправить ответ в чат {}", chatId, e);
        }
    }
}