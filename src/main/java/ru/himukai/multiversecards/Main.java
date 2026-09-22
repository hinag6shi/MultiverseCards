package ru.himukai.multiversecards;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cdimascio.dotenv.Dotenv;
import okhttp3.OkHttpClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.longpolling.util.TelegramOkHttpClientFactory;
import ru.himukai.multiversecards.platform.telegram.TelegramBot;
import ru.himukai.multiversecards.platform.telegram.commands.AboutCommand;
import ru.himukai.multiversecards.platform.telegram.commands.AuthorCommand;
import ru.himukai.multiversecards.platform.telegram.commands.HelpCommand;
import ru.himukai.multiversecards.platform.telegram.core.CommandDispatcher;
import ru.himukai.multiversecards.platform.telegram.core.CommandRegistry;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.function.Supplier;

public final class Main {

    public static void main(String[] args) throws Exception {
        Dotenv dotenv = Dotenv.load();

        String botToken = dotenv.get("BOT_TOKEN");

        if (botToken == null || botToken.isBlank()) {
            throw new IllegalStateException(
                    "Не задана переменная BOT_TOKEN."
            );
        }

        boolean proxyEnabled = Boolean.parseBoolean(
                dotenv.get("PROXY_ENABLED", "false")
        );

        String proxyHost = dotenv.get(
                "PROXY_HOST",
                "127.0.0.1"
        );

        int proxyPort = Integer.parseInt(
                dotenv.get("PROXY_PORT", "2080")
        );

        Supplier<OkHttpClient> httpClientCreator;

        if (proxyEnabled) {
            Proxy proxy = new Proxy(
                    Proxy.Type.HTTP,
                    new InetSocketAddress(proxyHost, proxyPort)
            );

            httpClientCreator =
                    new TelegramOkHttpClientFactory.HttpProxyOkHttpClientCreator(
                            () -> proxy,
                            () -> null
                    );

            System.out.printf(
                    "HTTP proxy enabled: %s:%d%n",
                    proxyHost,
                    proxyPort
            );
        } else {
            httpClientCreator =
                    new TelegramOkHttpClientFactory.DefaultOkHttpClientCreator();

            System.out.println("HTTP proxy disabled");
        }

        OkHttpClient httpClient = httpClientCreator.get();

        System.out.println("BOT_TOKEN loaded: true");

        CommandRegistry registry = new CommandRegistry();

        HelpCommand help = new HelpCommand(registry);

        registry.register(
                help,
                new AuthorCommand(),
                new AboutCommand()
        );

        CommandDispatcher dispatcher =
                new CommandDispatcher(registry);

        TelegramBot bot = new TelegramBot(
                botToken,
                dispatcher,
                httpClient
        );

        try (
                TelegramBotsLongPollingApplication application =
                        new TelegramBotsLongPollingApplication(
                                ObjectMapper::new,
                                () -> httpClient
                        )
        ) {

            application.registerBot(
                    botToken,
                    bot
            );

            Thread.currentThread().join();
        }
    }
}