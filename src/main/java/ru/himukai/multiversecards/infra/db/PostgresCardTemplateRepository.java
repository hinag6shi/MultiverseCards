package ru.himukai.multiversecards.infra.db;

import ru.himukai.multiversecards.domain.card.CardTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

/** Copies the JSON card catalog into PostgreSQL; JSON remains the source of truth. */
public final class PostgresCardTemplateRepository {

    private static final String UPSERT = """
            INSERT INTO card_templates (id, name, universe, rarity, base_hp, base_st, base_df, is_starter)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                name = EXCLUDED.name,
                universe = EXCLUDED.universe,
                rarity = EXCLUDED.rarity,
                base_hp = EXCLUDED.base_hp,
                base_st = EXCLUDED.base_st,
                base_df = EXCLUDED.base_df,
                is_starter = EXCLUDED.is_starter
            """;

    private final DataSource dataSource;

    public PostgresCardTemplateRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    public void synchronize(List<CardTemplate> cards) {
        Objects.requireNonNull(cards, "cards");
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(UPSERT)) {
                for (CardTemplate card : cards) {
                    statement.setString(1, card.id().value());
                    statement.setString(2, card.name());
                    statement.setString(3, card.universe());
                    statement.setString(4, card.rarity().name());
                    statement.setInt(5, card.baseStats().hp());
                    statement.setInt(6, card.baseStats().st());
                    statement.setInt(7, card.baseStats().df());
                    statement.setBoolean(8, card.starter());
                    statement.addBatch();
                }
                statement.executeBatch();
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось синхронизировать шаблоны карточек с PostgreSQL", e);
        }
    }
}
