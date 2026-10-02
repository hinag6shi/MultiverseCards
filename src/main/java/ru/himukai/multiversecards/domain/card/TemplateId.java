package ru.himukai.multiversecards.domain.card;

import java.util.Objects;

public record TemplateId(String value) {
    public TemplateId {
        Objects.requireNonNull(value, "value");

        if (value.isBlank()) {
            throw new IllegalArgumentException("value не может быть пустым");
        }
    }
}