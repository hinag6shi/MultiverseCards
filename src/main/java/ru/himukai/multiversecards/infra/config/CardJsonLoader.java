package ru.himukai.multiversecards.infra.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.himukai.multiversecards.domain.card.CardTemplate;
import ru.himukai.multiversecards.domain.card.Rarity;
import ru.himukai.multiversecards.domain.card.Stats;
import ru.himukai.multiversecards.domain.card.TemplateId;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/** Loads universe card files from the classpath's cards/ directory. */
public final class CardJsonLoader {

    private static final String RESOURCE_DIRECTORY = "cards";

    private CardJsonLoader() {
    }

    public static List<CardTemplate> load(ObjectMapper mapper) {
        Objects.requireNonNull(mapper, "mapper");
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = CardJsonLoader.class.getClassLoader();
        }
        List<CardTemplate> cards = new ArrayList<>();
        Set<String> ids = new HashSet<>();

        try {
            Enumeration<URL> directories = classLoader.getResources(RESOURCE_DIRECTORY);
            while (directories.hasMoreElements()) {
                URL directory = directories.nextElement();
                if (directory.getProtocol().equals("file")) {
                    loadDirectory(mapper, Path.of(directory.toURI()), cards, ids);
                } else if (directory.getProtocol().equals("jar")) {
                    loadJar(mapper, directory, cards, ids);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось загрузить JSON-каталог карточек из resources/cards", e);
        }

        if (cards.isEmpty()) {
            throw new IllegalStateException("В resources/cards не найдено карточек в JSON-файлах");
        }
        for (CardTemplate card : cards) {
            if (classLoader.getResource(card.imageResource()) == null) {
                throw new IllegalStateException("Не найден файл изображения " + card.imageResource());
            }
        }

        return List.copyOf(cards);
    }

    private static void loadDirectory(ObjectMapper mapper, Path directory, List<CardTemplate> cards, Set<String> ids)
            throws IOException {
        try (Stream<Path> files = Files.list(directory)) {
            for (Path file : files.filter(path -> path.getFileName().toString().endsWith(".json")).sorted().toList()) {
                try (InputStream input = Files.newInputStream(file)) {
                    readFile(mapper, input, file.getFileName().toString(), cards, ids);
                }
            }
        }
    }

    private static void loadJar(ObjectMapper mapper, URL directory,
                                List<CardTemplate> cards, Set<String> ids) throws Exception {
        JarURLConnection connection = (JarURLConnection) directory.openConnection();
        String prefix = connection.getEntryName() == null ? "cards/" : connection.getEntryName() + "/";
        try (JarFile jar = connection.getJarFile()) {
            List<String> names = jar.stream()
                    .map(JarEntry::getName)
                    .filter(name -> name.startsWith(prefix) && name.endsWith(".json"))
                    .sorted()
                    .toList();
            for (String name : names) {
                JarEntry entry = jar.getJarEntry(name);
                if (entry == null) {
                    throw new IOException("Не найден ресурс " + name);
                }
                try (InputStream input = jar.getInputStream(entry)) {
                    readFile(mapper, input, name, cards, ids);
                }
            }
        }
    }

    private static void readFile(ObjectMapper mapper, InputStream input, String filename,
                                 List<CardTemplate> cards, Set<String> ids) throws IOException {
        JsonNode root = mapper.readTree(input);
        String universe = requiredText(root, "universe", filename);
        JsonNode cardNodes = root.path("cards");
        if (!cardNodes.isArray()) {
            throw new IllegalArgumentException(filename + ": поле cards должно быть массивом");
        }

        for (JsonNode card : cardNodes) {
            String id = requiredText(card, "id", filename);
            if (!ids.add(id)) {
                throw new IllegalArgumentException(filename + ": повторяющийся ID карточки " + id);
            }
            try {
                cards.add(new CardTemplate(
                        new TemplateId(id),
                        requiredText(card, "name", filename),
                        universe,
                        Rarity.valueOf(requiredText(card, "rarity", filename)),
                        new Stats(requiredInt(card, "hp", filename),
                                requiredInt(card, "st", filename),
                                requiredInt(card, "df", filename)),
                        requiredBoolean(card, "starter", filename)
                ));
            } catch (RuntimeException e) {
                throw new IllegalArgumentException(filename + ": некорректная карточка " + id, e);
            }
        }
    }

    private static String requiredText(JsonNode node, String field, String filename) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new IllegalArgumentException(filename + ": поле " + field + " должно быть непустой строкой");
        }
        return value.asText();
    }

    private static int requiredInt(JsonNode node, String field, String filename) {
        JsonNode value = node.get(field);
        if (value == null || !value.isIntegralNumber() || !value.canConvertToInt() || value.asInt() < 0) {
            throw new IllegalArgumentException(filename + ": поле " + field + " должно быть целым числом >= 0");
        }
        return value.asInt();
    }

    private static boolean requiredBoolean(JsonNode node, String field, String filename) {
        JsonNode value = node.get(field);
        if (value == null || !value.isBoolean()) {
            throw new IllegalArgumentException(filename + ": поле " + field + " должно быть true/false");
        }
        return value.asBoolean();
    }

}
