package ru.himukai.multiversecards.bot;

import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.himukai.multiversecards.core.Button;
import ru.himukai.multiversecards.core.CommandDispatcher;
import ru.himukai.multiversecards.core.Keyboard;
import ru.himukai.multiversecards.core.Response;
import ru.himukai.multiversecards.domain.player.Player;
import ru.himukai.multiversecards.domain.player.PlayerId;
import ru.himukai.multiversecards.service.PlayerService;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class TelegramBot implements LongPollingSingleThreadUpdateConsumer, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(TelegramBot.class);

    private static final String USER_ID_PREFIX = "tg:";
    /** Лимит callback_data в байтах. */
    private static final int MAX_CALLBACK_DATA_BYTES = 64;
    private static final String NOT_MODIFIED = "message is not modified";
    private static final long NAME_WAIT_SECONDS = 30;

    private final CommandDispatcher dispatcher;
    private final TelegramClient client;
    private final KeyedExecutor executor;
    private final PlayerService players;
    private final ConcurrentHashMap<Long, PendingName> pendingNames = new ConcurrentHashMap<>();
    private final ScheduledExecutorService nameTimeouts = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "name-registration-timeouts");
        thread.setDaemon(true);
        return thread;
    });

    public TelegramBot(String botToken, CommandDispatcher dispatcher, OkHttpClient httpClient,
                       KeyedExecutor executor, PlayerService players) {
        this.dispatcher = dispatcher;
        this.client = new OkHttpTelegramClient(httpClient, botToken);
        this.executor = executor;
        this.players = players;
    }

    /**
     * Вызывается потоком long polling и только раскладывает апдейты по очередям игроков:
     * тяжёлая работа (БД, запросы к Telegram) идёт уже в потоке игрока.
     * Апдейты одного игрока обрабатываются строго по порядку, разных игроков — параллельно.
     */
    @Override
    public void consume(Update update) {
        if (update.hasCallbackQuery()) {
            var query = update.getCallbackQuery();
            var message = query.getMessage();
            long userId = query.getFrom().getId();
            String queryId = query.getId();
            String data = query.getData();
            Long chatId = message == null ? null : message.getChatId();
            Integer messageId = message == null ? null : message.getMessageId();

            executor.submit(userId, () -> {
                answerCallback(queryId);
                if (data != null && chatId != null) {
                    reply(chatId, userId, data, messageId);
                }
            });
        } else if (update.hasMessage() && update.getMessage().hasText()) {
            var message = update.getMessage();
            if (message.getFrom() != null) {
                long userId = message.getFrom().getId();
                long chatId = message.getChatId();
                String text = message.getText();

                executor.submit(userId, () -> reply(chatId, userId, text, null));
            }
        }
    }

    /**
     * @param sourceMessageId сообщение, с кнопки которого пришла команда; {@code null}, если команду набрали вручную
     */
    private void reply(long chatId, long userId, String text, Integer sourceMessageId) {
        if (isStartCommand(text)) {
            Player player = players.find(new PlayerId(userId)).orElse(null);
            if (player == null) {
                beginNameWait(chatId, userId);
                return;
            }
            cancelNameWait(userId);
        }

        PendingName pending = pendingNames.get(userId);
        if (pending != null && !text.strip().startsWith("/")) {
            acceptName(userId, pending, text);
            return;
        }

        if (!text.strip().startsWith("/") && players.find(new PlayerId(userId)).isEmpty()) {
            sendNew(String.valueOf(chatId), "Сначала запусти /start, затем отправь имя в течение 30 секунд.", null);
            return;
        }

        Response response = dispatcher.handle(USER_ID_PREFIX + userId, text);
        String chat = String.valueOf(chatId);
        if (response.photoResource() != null) {
            sendPhoto(chat, response.text(), response.photoResource());
            return;
        }
        InlineKeyboardMarkup markup = toMarkup(response.keyboard());

        boolean edit = response.renderMode() == Response.RenderMode.UPDATE && sourceMessageId != null;
        if (edit && tryEdit(chat, sourceMessageId, response.text(), markup)) {
            return;
        }
        sendNew(chat, response.text(), markup);
    }

    private void beginNameWait(long chatId, long userId) {
        cancelNameWait(userId);
        PendingName pending = new PendingName(chatId, System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(NAME_WAIT_SECONDS));
        pendingNames.put(userId, pending);
        pending.timeout = nameTimeouts.schedule(
                () -> executor.submit(userId, () -> expireNameWait(userId, pending)),
                NAME_WAIT_SECONDS, TimeUnit.SECONDS);
        sendNew(String.valueOf(chatId), "Привет! 😊 Добро пожаловать в Multiverse Cards 🃏\n"
                + "Как мне к тебе обращаться? Отправь имя отдельным сообщением в течение 30 секунд.", null);
    }

    private void acceptName(long userId, PendingName pending, String rawName) {
        if (System.currentTimeMillis() >= pending.deadlineMillis) {
            expireNameWait(userId, pending);
            return;
        }
        String name = rawName.strip();
        if (name.isBlank()) {
            sendNew(String.valueOf(pending.chatId), "Имя пустое. Отправь имя в течение оставшегося времени.", null);
            return;
        }
        if (!pendingNames.remove(userId, pending)) {
            return;
        }
        if (pending.timeout != null) {
            pending.timeout.cancel(false);
        }
        Player player = players.start(new PlayerId(userId), name);
        sendNew(String.valueOf(pending.chatId), "Приятно познакомиться, " + player.name() + "! ✨\n"
                + "Стартовый набор выдан! Посмотреть: /cards", null);
    }

    private void expireNameWait(long userId, PendingName pending) {
        if (pendingNames.remove(userId, pending)) {
            sendNew(String.valueOf(pending.chatId), "30 секунд истекли. Запусти /start заново и отправь имя.", null);
        }
    }

    private void cancelNameWait(long userId) {
        PendingName pending = pendingNames.remove(userId);
        if (pending != null && pending.timeout != null) {
            pending.timeout.cancel(false);
        }
    }

    private static boolean isStartCommand(String text) {
        String first = text.strip().split("\\s+", 2)[0].split("@", 2)[0];
        return first.equalsIgnoreCase("/start");
    }

    private static final class PendingName {
        private final long chatId;
        private final long deadlineMillis;
        private volatile ScheduledFuture<?> timeout;

        private PendingName(long chatId, long deadlineMillis) {
            this.chatId = chatId;
            this.deadlineMillis = deadlineMillis;
        }
    }

    /** @return {@code true}, если сообщение больше не нужно отправлять заново */
    private boolean tryEdit(String chatId, int messageId, String text, InlineKeyboardMarkup markup) {
        try {
            client.execute(EditMessageText.builder()
                    .chatId(chatId).messageId(messageId).text(text).replyMarkup(markup).build());
            return true;
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains(NOT_MODIFIED)) {
                return true;
            }
            log.warn("Не удалось отредактировать сообщение {} в чате {}, отправляю новое", messageId, chatId, e);
            return false;
        }
    }

    private void sendNew(String chatId, String text, InlineKeyboardMarkup markup) {
        try {
            client.execute(SendMessage.builder().chatId(chatId).text(text).replyMarkup(markup).build());
        } catch (Exception e) {
            log.error("Не удалось отправить ответ в чат {}", chatId, e);
        }
    }

    private void sendPhoto(String chatId, String caption, String resource) {
        try (InputStream input = TelegramBot.class.getClassLoader().getResourceAsStream(resource)) {
            if (input == null) {
                log.error("Не найден ресурс изображения {}", resource);
                sendNew(chatId, caption, null);
                return;
            }
            String filename = resource.substring(resource.lastIndexOf('/') + 1);
            client.execute(SendPhoto.builder()
                    .chatId(chatId)
                    .photo(new InputFile(input, filename))
                    .caption(caption)
                    .build());
        } catch (Exception e) {
            log.error("Не удалось отправить изображение {} в чат {}", resource, chatId, e);
        }
    }

    private InlineKeyboardMarkup toMarkup(Keyboard keyboard) {
        List<InlineKeyboardRow> rows = keyboard.rows().stream()
                .map(row -> row.stream().filter(this::fitsCallbackData).map(TelegramBot::toInline).toList())
                .filter(row -> !row.isEmpty())
                .map(InlineKeyboardRow::new)
                .toList();
        return rows.isEmpty() ? null : InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    private boolean fitsCallbackData(Button button) {
        boolean fits = button.commandText().getBytes(StandardCharsets.UTF_8).length <= MAX_CALLBACK_DATA_BYTES;
        if (!fits) {
            log.warn("Кнопка «{}» пропущена: команда длиннее {} байт", button.label(), MAX_CALLBACK_DATA_BYTES);
        }
        return fits;
    }

    private static InlineKeyboardButton toInline(Button button) {
        return InlineKeyboardButton.builder().text(button.label()).callbackData(button.commandText()).build();
    }

    private void answerCallback(String callbackQueryId) {
        try {
            client.execute(AnswerCallbackQuery.builder().callbackQueryId(callbackQueryId).build());
        } catch (Exception e) {
            log.warn("Не удалось ответить на нажатие кнопки {}", callbackQueryId, e);
        }
    }

    @Override
    public void close() {
        nameTimeouts.shutdownNow();
        pendingNames.clear();
    }
}
