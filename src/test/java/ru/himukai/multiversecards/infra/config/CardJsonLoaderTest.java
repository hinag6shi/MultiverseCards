package ru.himukai.multiversecards.infra.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.himukai.multiversecards.domain.card.CardTemplate;
import ru.himukai.multiversecards.domain.card.Rarity;
import ru.himukai.multiversecards.domain.card.Stats;
import ru.himukai.multiversecards.domain.card.TemplateId;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CardJsonLoaderTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @TempDir
    Path tempDir;

    @Test
    void shouldLoadCardsFromJsonAndResolveTheirImages() throws Exception {
        Path resources = createResourceDirectory();
        writeJson(resources, "Sanrio.json", """
                {
                  "universe": "Sanrio",
                  "cards": [
                    {
                      "id": "sanrio_hello_kitty_divine",
                      "name": "Hello Kitty",
                      "rarity": "DIVINE",
                      "hp": 5120,
                      "st": 1472,
                      "df": 512,
                      "starter": false
                    }
                  ]
                }
                """);
        writeImage(resources, "cards/images/sanrio_hello_kitty_divine.png");

        List<CardTemplate> cards = withContextClassLoader(resources, () -> CardJsonLoader.load(MAPPER));

        assertEquals(List.of(new CardTemplate(
                new TemplateId("sanrio_hello_kitty_divine"),
                "Hello Kitty",
                "Sanrio",
                Rarity.DIVINE,
                new Stats(5120, 1472, 512),
                false
        )), cards);
        assertThrows(UnsupportedOperationException.class, () -> cards.add(cards.getFirst()));
    }

    @Test
    void shouldRejectDuplicateCardIdsAcrossJsonFiles() throws Exception {
        Path resources = createResourceDirectory();
        String card = cardJson("same_id", "COMMON", 10, 5, 3, false);
        writeJson(resources, "A.json", universeJson("First", card));
        writeJson(resources, "B.json", universeJson("Second", card));
        writeImage(resources, "cards/images/same_id.png");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> withContextClassLoader(resources, () -> CardJsonLoader.load(MAPPER)));

        assertTrue(rootMessage(error).contains("повторяющийся ID карточки same_id"));
    }

    @Test
    void shouldRejectInvalidCardFields() throws Exception {
        Path resources = createResourceDirectory();
        writeJson(resources, "Invalid.json", universeJson("Sanrio",
                cardJson("invalid", "COMMON", -1, 5, 3, false)));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> withContextClassLoader(resources, () -> CardJsonLoader.load(MAPPER)));

        assertTrue(rootMessage(error).contains("поле hp должно быть целым числом >= 0"));
    }

    @Test
    void shouldRejectCardsWithoutImages() throws Exception {
        Path resources = createResourceDirectory();
        writeJson(resources, "Sanrio.json", universeJson("Sanrio",
                cardJson("missing_image", "COMMON", 10, 5, 3, false)));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> withContextClassLoader(resources, () -> CardJsonLoader.load(MAPPER)));

        assertTrue(error.getMessage().contains("Не найден файл изображения cards/images/missing_image.png"));
    }

    @Test
    void shouldRejectAnEmptyJsonCatalog() throws Exception {
        Path resources = createResourceDirectory();
        writeJson(resources, "Empty.json", "{\"universe\":\"Sanrio\",\"cards\":[]}");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> withContextClassLoader(resources, () -> CardJsonLoader.load(MAPPER)));

        assertTrue(error.getMessage().contains("не найдено карточек"));
    }

    private Path createResourceDirectory() throws IOException {
        Path resources = tempDir.resolve("resources");
        Files.createDirectories(resources.resolve("cards"));
        return resources;
    }

    private static String universeJson(String universe, String card) {
        return "{\"universe\":\"" + universe + "\",\"cards\":[" + card + "]}";
    }

    private static String cardJson(String id, String rarity, int hp, int st, int df, boolean starter) {
        return "{\"id\":\"" + id + "\",\"name\":\"Card\",\"rarity\":\"" + rarity
                + "\",\"hp\":" + hp + ",\"st\":" + st + ",\"df\":" + df
                + ",\"starter\":" + starter + "}";
    }

    private static void writeJson(Path resources, String filename, String json) throws IOException {
        Files.writeString(resources.resolve("cards").resolve(filename), json);
    }

    private static void writeImage(Path resources, String resourcePath) throws IOException {
        Path image = resources.resolve(resourcePath);
        Files.createDirectories(image.getParent());
        Files.createFile(image);
    }

    private static String rootMessage(Throwable error) {
        Throwable root = error;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return root.getMessage();
    }

    private static <T> T withContextClassLoader(Path resources, CheckedSupplier<T> action) throws Exception {
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        try (URLClassLoader testClassLoader = new URLClassLoader(new URL[]{resources.toUri().toURL()}, null)) {
            Thread.currentThread().setContextClassLoader(testClassLoader);
            return action.get();
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }

    @FunctionalInterface
    private interface CheckedSupplier<T> {
        T get() throws Exception;
    }
}
