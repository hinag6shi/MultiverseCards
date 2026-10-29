package ru.himukai.multiversecards.service;

import ru.himukai.multiversecards.domain.card.CardTemplate;
import ru.himukai.multiversecards.domain.card.TemplateId;

import java.util.List;
import java.util.Optional;

public interface CardCatalog {
    Optional<CardTemplate> find(TemplateId id);

    List<CardTemplate> all();

    List<CardTemplate> starterSet();
}