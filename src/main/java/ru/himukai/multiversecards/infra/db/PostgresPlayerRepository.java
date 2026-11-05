package ru.himukai.multiversecards.infra.db;

import ru.himukai.multiversecards.domain.card.Boosts;
import ru.himukai.multiversecards.domain.card.CardTemplate;
import ru.himukai.multiversecards.domain.card.Rarity;
import ru.himukai.multiversecards.domain.card.Stats;
import ru.himukai.multiversecards.domain.card.TemplateId;
import ru.himukai.multiversecards.domain.player.CardCollection;
import ru.himukai.multiversecards.domain.player.OwnedCard;
import ru.himukai.multiversecards.domain.player.Player;
import ru.himukai.multiversecards.domain.player.PlayerId;
import ru.himukai.multiversecards.service.PlayerRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class PostgresPlayerRepository implements PlayerRepository {

    private static final String SELECT_PLAYER = "SELECT name, cash FROM players WHERE id = ?";

    private static final String SELECT_CARDS = """
            SELECT t.id AS template_id, t.name, t.universe, t.rarity,
                   t.base_hp, t.base_st, t.base_df,
                   pc.hp_boost, pc.st_boost, pc.df_boost, pc.shards, pc.obtained_at
            FROM player_cards pc
            JOIN card_templates t ON t.id = pc.template_id
            WHERE pc.player_id = ?
            ORDER BY pc.obtained_at, t.id
            """;

    private static final String UPSERT_PLAYER = """
            INSERT INTO players (id, name, cash) VALUES (?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, cash = EXCLUDED.cash
            """;

    /** Усиления и осколки меняются, поэтому обновляем; дата получения остаётся прежней. */
    private static final String UPSERT_CARD = """
            INSERT INTO player_cards (player_id, template_id, hp_boost, st_boost, df_boost, shards, obtained_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (player_id, template_id) DO UPDATE SET
                hp_boost = EXCLUDED.hp_boost,
                st_boost = EXCLUDED.st_boost,
                df_boost = EXCLUDED.df_boost,
                shards   = EXCLUDED.shards
            """;

    private final DataSource dataSource;

    public PostgresPlayerRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    @Override
    public Optional<Player> find(PlayerId id) {
        try (Connection conn = dataSource.getConnection()) {
            String name;
            long cash;
            try (PreparedStatement ps = conn.prepareStatement(SELECT_PLAYER)) {
                ps.setLong(1, id.value());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    name = rs.getString("name");
                    cash = rs.getLong("cash");
                }
            }
            return Optional.of(new Player(id, name, cash, new CardCollection(loadCards(conn, id))));
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось загрузить игрока " + id.value(), e);
        }
    }

    @Override
    public void save(Player player) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                upsertPlayer(conn, player);
                upsertCards(conn, player);
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось сохранить игрока " + player.id().value(), e);
        }
    }

    private static List<OwnedCard> loadCards(Connection conn, PlayerId id) throws SQLException {
        List<OwnedCard> cards = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT_CARDS)) {
            ps.setLong(1, id.value());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CardTemplate template = new CardTemplate(
                            new TemplateId(rs.getString("template_id")),
                            rs.getString("name"),
                            rs.getString("universe"),
                            Rarity.valueOf(rs.getString("rarity")),
                            new Stats(rs.getInt("base_hp"), rs.getInt("base_st"), rs.getInt("base_df"))
                    );
                    Boosts boosts = new Boosts(rs.getInt("hp_boost"), rs.getInt("st_boost"), rs.getInt("df_boost"));
                    cards.add(new OwnedCard(
                            template,
                            boosts,
                            rs.getInt("shards"),
                            rs.getObject("obtained_at", OffsetDateTime.class).toInstant()
                    ));
                }
            }
        }
        return cards;
    }

    private static void upsertPlayer(Connection conn, Player player) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPSERT_PLAYER)) {
            ps.setLong(1, player.id().value());
            ps.setString(2, player.name());
            ps.setLong(3, player.cash());
            ps.executeUpdate();
        }
    }

    private static void upsertCards(Connection conn, Player player) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPSERT_CARD)) {
            for (OwnedCard card : player.cards().all()) {
                ps.setLong(1, player.id().value());
                ps.setString(2, card.templateId().value());
                ps.setInt(3, card.boosts().hp());
                ps.setInt(4, card.boosts().st());
                ps.setInt(5, card.boosts().df());
                ps.setInt(6, card.shards());
                ps.setObject(7, card.obtainedAt().atOffset(ZoneOffset.UTC));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}
