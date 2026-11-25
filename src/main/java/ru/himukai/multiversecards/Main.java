package ru.himukai.multiversecards;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cdimascio.dotenv.Dotenv;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.longpolling.util.TelegramOkHttpClientFactory;
import ru.himukai.multiversecards.bot.KeyedExecutor;
import ru.himukai.multiversecards.bot.TelegramBot;
import ru.himukai.multiversecards.commands.AboutCommand;
import ru.himukai.multiversecards.commands.AuthorCommand;
import ru.himukai.multiversecards.commands.CardsCommand;
import ru.himukai.multiversecards.commands.HelpCommand;
import ru.himukai.multiversecards.commands.NameCommand;
import ru.himukai.multiversecards.commands.StartCommand;
import ru.himukai.multiversecards.commands.UpgradeCommand;
import ru.himukai.multiversecards.core.CommandDispatcher;
import ru.himukai.multiversecards.core.CommandRegistry;
import ru.himukai.multiversecards.domain.card.UpgradeRules;
import ru.himukai.multiversecards.infra.config.CardJsonLoader;
import ru.himukai.multiversecards.infra.config.UpgradeBalanceLoader;
import ru.himukai.multiversecards.infra.db.Database;
import ru.himukai.multiversecards.infra.db.PostgresCardTemplateRepository;
import ru.himukai.multiversecards.infra.db.PostgresPlayerRepository;
import ru.himukai.multiversecards.service.CardCatalog;
import ru.himukai.multiversecards.service.CardUpgradeService;
import ru.himukai.multiversecards.service.DefaultCardCatalog;
import ru.himukai.multiversecards.service.DefaultPlayerService;
import ru.himukai.multiversecards.service.PlayerService;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.function.Supplier;

public final class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    /** Сколько игроков бот обрабатывает параллельно; соединений с БД должно хватать на всех */
    private static final int USER_LANES = 8;

    private Main() {
    }

    public static void main(String[] args) throws Exception {
        // .env можно не заводить — тогда берём настройки из переменных окружения
        Dotenv env = Dotenv.configure().ignoreIfMissing().load();

        String botToken = env.get("BOT_TOKEN");
        if (botToken == null || botToken.isBlank()) {
            throw new IllegalStateException("Не задана переменная BOT_TOKEN.");
        }

        OkHttpClient httpClient = createHttpClient(env);

        // Сначала перестаём принимать апдейты, потом разбираем очередь и в конце закрываем БД
        try (Database database = Database.connect(env);
             KeyedExecutor executor = new KeyedExecutor(USER_LANES)) {

            ObjectMapper mapper = new ObjectMapper();
            var templates = CardJsonLoader.load(mapper);
            var upgradeBalance = UpgradeBalanceLoader.load(mapper);
            UpgradeRules upgradeRules = new UpgradeRules(upgradeBalance);
            new PostgresCardTemplateRepository(database.dataSource()).synchronize(templates);
            CardCatalog catalog = new DefaultCardCatalog(templates);
            log.info("Загружено шаблонов карточек: {}", templates.size());

            var playerRepository = new PostgresPlayerRepository(database.dataSource());
            PlayerService players = new DefaultPlayerService(playerRepository, catalog);
            CardUpgradeService cardUpgrades = new CardUpgradeService(playerRepository, upgradeBalance);
            CommandDispatcher dispatcher = new CommandDispatcher(
                    createRegistry(players, upgradeRules, cardUpgrades));
            try (TelegramBot bot = new TelegramBot(botToken, dispatcher, httpClient, executor, players)) {
                try (TelegramBotsLongPollingApplication application =
                             new TelegramBotsLongPollingApplication(ObjectMapper::new, () -> httpClient)) {
                    application.registerBot(botToken, bot);
                    log.info("Бот запущен");
                    Thread.currentThread().join();
                }
            }
        }
    }

    private static CommandRegistry createRegistry(PlayerService players, UpgradeRules upgradeRules,
                                                 CardUpgradeService cardUpgrades) {
        CommandRegistry registry = new CommandRegistry();
        return registry.register(
                new HelpCommand(registry, players),
                new AuthorCommand(players),
                new AboutCommand(players),
                new NameCommand(players),
                new CardsCommand(players, upgradeRules),
                new UpgradeCommand(cardUpgrades),
                new StartCommand(players)
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
