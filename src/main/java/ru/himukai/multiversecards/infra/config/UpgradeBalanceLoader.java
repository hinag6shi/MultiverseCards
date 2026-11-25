package ru.himukai.multiversecards.infra.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.himukai.multiversecards.domain.card.UpgradeBalance;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/** Берёт правила усилений из JSON с балансом */
public final class UpgradeBalanceLoader {

    private static final String RESOURCE = "balance/upgrade-rules.json";

    private UpgradeBalanceLoader() {
    }

    public static UpgradeBalance load(ObjectMapper mapper) {
        Objects.requireNonNull(mapper, "mapper");
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = UpgradeBalanceLoader.class.getClassLoader();
        }
        try (InputStream input = classLoader.getResourceAsStream(RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Не найден файл баланса " + RESOURCE);
            }
            return mapper.readValue(input, UpgradeBalance.class);
        } catch (IOException | IllegalArgumentException e) {
            throw new IllegalStateException("Не удалось загрузить баланс усилений из " + RESOURCE, e);
        }
    }
}
