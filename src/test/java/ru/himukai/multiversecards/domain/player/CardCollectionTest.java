package ru.himukai.multiversecards.domain.player;

import org.junit.jupiter.api.Test;
import ru.himukai.multiversecards.domain.card.Boosts;
import ru.himukai.multiversecards.domain.card.CardTemplate;
import ru.himukai.multiversecards.domain.card.Rarity;
import ru.himukai.multiversecards.domain.card.Stats;
import ru.himukai.multiversecards.domain.card.TemplateId;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CardCollectionTest {

    private static final Instant NOW = Instant.parse("2026-11-05T10:00:00Z");

    private static CardTemplate template(String id) {
        return new CardTemplate(new TemplateId(id), "Имя " + id, "Вселенная", Rarity.COMMON, new Stats(10, 5, 3));
    }

    private static OwnedCard card(String id) {
        return OwnedCard.fresh(template(id), NOW);
    }

    @Test
    void newCollectionIsEmpty() {
        assertEquals(0, new CardCollection().size());
        assertTrue(new CardCollection().all().isEmpty());
    }

    @Test
    void addKeepsInsertionOrder() {
        CardCollection collection = new CardCollection();
        collection.add(card("b"));
        collection.add(card("a"));

        assertEquals(List.of("b", "a"), collection.all().stream().map(c -> c.templateId().value()).toList());
        assertEquals(2, collection.size());
    }

    @Test
    void secondCopyOfSameTemplateIsRejected() {
        CardCollection collection = new CardCollection();
        collection.add(card("spiderman"));

        assertThrows(IllegalArgumentException.class, () -> collection.add(card("spiderman")));
        assertEquals(1, collection.size());
    }

    @Test
    void constructorRejectsDuplicates() {
        assertThrows(IllegalArgumentException.class,
                () -> new CardCollection(List.of(card("a"), card("a"))));
    }

    @Test
    void findAndContainsWorkByTemplateId() {
        CardCollection collection = new CardCollection(List.of(card("a")));

        assertTrue(collection.contains(new TemplateId("a")));
        assertFalse(collection.contains(new TemplateId("b")));
        assertTrue(collection.find(new TemplateId("a")).isPresent());
        assertTrue(collection.find(new TemplateId("b")).isEmpty());
    }

    @Test
    void updateReplacesExistingCard() {
        CardCollection collection = new CardCollection(List.of(card("a")));

        collection.update(collection.find(new TemplateId("a")).orElseThrow().withShards(3));

        assertEquals(3, collection.find(new TemplateId("a")).orElseThrow().shards());
        assertEquals(1, collection.size());
    }

    @Test
    void updateOfMissingCardIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new CardCollection().update(card("a")));
    }

    @Test
    void allIsReadOnlySnapshot() {
        CardCollection collection = new CardCollection(List.of(card("a")));

        assertThrows(UnsupportedOperationException.class, () -> collection.all().clear());
    }

    @Test
    void freshCardHasNoBoostsAndNoShards() {
        OwnedCard fresh = card("a");

        assertEquals(Boosts.NONE, fresh.boosts());
        assertEquals(0, fresh.shards());
        assertEquals(NOW, fresh.obtainedAt());
    }

    @Test
    void ownedCardRejectsNegativeShardsAndBoosts() {
        assertThrows(IllegalArgumentException.class, () -> card("a").withShards(-1));
        assertThrows(IllegalArgumentException.class, () -> new Boosts(0, -1, 0));
    }
}
