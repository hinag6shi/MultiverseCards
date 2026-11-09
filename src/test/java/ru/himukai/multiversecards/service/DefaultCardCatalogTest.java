
package ru.himukai.multiversecards.service;

import org.junit.jupiter.api.Test;
import ru.himukai.multiversecards.domain.card.CardTemplate;
import ru.himukai.multiversecards.domain.card.Rarity;
import ru.himukai.multiversecards.domain.card.Stats;
import ru.himukai.multiversecards.domain.card.TemplateId;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DefaultCardCatalogTest {

    private CardTemplate card(String id, boolean starter) {
        return new CardTemplate(
                new TemplateId(id),
                "Имя " + id,
                "Sunrio",
                Rarity.COMMON,
                new Stats(100, 20, 10),
                starter
        );
    }

    @Test
    void shouldReturnCardById() {
        CardTemplate card = card("sunrio_one", false);
        DefaultCardCatalog catalog = new DefaultCardCatalog(List.of(card));

        assertEquals(card, catalog.find(card.id()).orElseThrow());
    }

    @Test
    void shouldReturnEmptyForUnknownId() {
        DefaultCardCatalog catalog = new DefaultCardCatalog(List.of());

        assertTrue(catalog.find(new TemplateId("unknown")).isEmpty());
    }

    @Test
    void shouldReturnAllCardsInInsertionOrder() {
        CardTemplate first = card("sunrio_one", false);
        CardTemplate second = card("sunrio_two", true);

        DefaultCardCatalog catalog =
                new DefaultCardCatalog(List.of(first, second));

        assertEquals(List.of(first, second), catalog.all());
    }

    @Test
    void shouldReturnOnlyStarterCards() {
        CardTemplate first = card("sunrio_one", true);
        CardTemplate second = card("sunrio_two", false);
        CardTemplate third = card("sunrio_three", true);

        DefaultCardCatalog catalog =
                new DefaultCardCatalog(List.of(first, second, third));

        assertEquals(List.of(first, third), catalog.starterSet());
    }

    @Test
    void shouldRejectDuplicateIds() {
        CardTemplate first = card("sunrio_one", false);
        CardTemplate duplicate = card("sunrio_one", true);

        assertThrows(
                IllegalArgumentException.class,
                () -> new DefaultCardCatalog(List.of(first, duplicate))
        );
    }

    @Test
    void shouldNotReflectChangesToSourceCollection() {
        List<CardTemplate> source = new ArrayList<>();
        source.add(card("sunrio_one", false));

        DefaultCardCatalog catalog = new DefaultCardCatalog(source);

        source.add(card("sunrio_two", false));

        assertEquals(1, catalog.all().size());
    }

    @Test
    void shouldReturnImmutableLists() {
        DefaultCardCatalog catalog =
                new DefaultCardCatalog(List.of(card("sunrio_one", true)));

        assertThrows(
                UnsupportedOperationException.class,
                () -> catalog.all().clear()
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> catalog.starterSet().clear()
        );
    }
}
