package ru.himukai.multiversecards.domain.card;

import java.util.Objects;

public final class Card {
    private final CardId id;
    private final CardTemplate template;

    public Card(CardId id, CardTemplate template) {
        this.id = Objects.requireNonNull(id, "id");
        this.template = Objects.requireNonNull(template, "template");
    }

    public CardId id() {
        return id;
    }

    public CardTemplate template() {
        return template;
    }

    public TemplateId templateId() {
        return template.id();
    }
}