package ru.himukai.multiversecards;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cdimascio.dotenv.Dotenv;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.longpolling.util.TelegramOkHttpClientFactory;
import ru.himukai.multiversecards.bot.TelegramBot;
import ru.himukai.multiversecards.commands.AboutCommand;
import ru.himukai.multiversecards.commands.AuthorCommand;
import ru.himukai.multiversecards.commands.HelpCommand;
import ru.himukai.multiversecards.commands.StartCommand;
import ru.himukai.multiversecards.core.CommandDispatcher;
import ru.himukai.multiversecards.core.CommandRegistry;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.function.Supplier;

public final class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private Main() {
    }

    public static void main(String[] args) throws Exception {
        // .env необязателен: без него значения берутся из переменных окружения
        Dotenv env = Dotenv.configure().ignoreIfMissing().load();

        String botToken = env.get("BOT_TOKEN");
        if (botToken == null || botToken.isBlank()) {
            throw new IllegalStateException("Не задана переменная BOT_TOKEN.");
        }

        OkHttpClient httpClient = createHttpClient(env);
        CommandDispatcher dispatcher = new CommandDispatcher(createRegistry());
        TelegramBot bot = new TelegramBot(botToken, dispatcher, httpClient);

        try (TelegramBotsLongPollingApplication application =
                     new TelegramBotsLongPollingApplication(ObjectMapper::new, () -> httpClient)) {
            application.registerBot(botToken, bot);
            log.info("Бот запущен");
            Thread.currentThread().join();
        }
    }

    private static CommandRegistry createRegistry() {
        CommandRegistry registry = new CommandRegistry();
        return registry.register(
                new HelpCommand(registry),
                new AuthorCommand(),
                new AboutCommand(),
                new StartCommand()
        );
    }

    private static OkHttpClient createHttpClient(Dotenv env) {
        boolean proxyEnabled = Boolean.parseBoolean(env.get("PROXY_ENABLED", "false"));
        if (!proxyEnabled) {
            log.info("HTTP proxy disabled");
            return new TelegramOkHttpClientFactory.DefaultOkHttpClientCreator().get();
        }

        String host = env.get("PROXY_HOST", "127.0.0.1");
        int port = Integer.parseInt(env.get("PROXY_PORT", "2080"));
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
        Supplier<OkHttpClient> creator =
                new TelegramOkHttpClientFactory.HttpProxyOkHttpClientCreator(() -> proxy, () -> null);
        log.info("HTTP proxy enabled: {}:{}", host, port);
        return creator.get();
    }
}
