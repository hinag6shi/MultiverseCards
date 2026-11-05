package ru.himukai.multiversecards.infra.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.cdimascio.dotenv.Dotenv;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

/** Пул соединений с PostgreSQL + применение миграций (db/migration) при старте. */
public final class Database implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(Database.class);

    private final HikariDataSource dataSource;

    private Database(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public static Database connect(Dotenv env) {
        String host = env.get("DB_HOST", "localhost");
        String port = env.get("DB_PORT", "5432");
        String name = required(env, "DB_NAME");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + name);
        config.setUsername(required(env, "DB_USER"));
        config.setPassword(required(env, "DB_PASSWORD"));
        config.setPoolName("multiverse-cards");
        config.setMaximumPoolSize(10);

        HikariDataSource ds;
        try {
            ds = new HikariDataSource(config);
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                    "Не удалось подключиться к БД " + host + ":" + port + "/" + name
                            + ". Запущен ли контейнер (docker compose up -d)?", e);
        }

        Flyway.configure().dataSource(ds).load().migrate();
        log.info("БД подключена: {}:{}/{}", host, port, name);
        return new Database(ds);
    }

    public DataSource dataSource() {
        return dataSource;
    }

    @Override
    public void close() {
        dataSource.close();
    }

    private static String required(Dotenv env, String key) {
        String value = env.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Не задана переменная " + key + ".");
        }
        return value;
    }
}
