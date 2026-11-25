package ru.himukai.multiversecards.infra.db;

import ru.himukai.multiversecards.domain.card.Boosts;
import ru.himukai.multiversecards.domain.card.CardTemplate;
import ru.himukai.multiversecards.domain.card.Rarity;
import ru.himukai.multiversecards.domain.card.Stats;
import ru.himukai.multiversecards.domain.card.TemplateId;
import ru.himukai.multiversecards.domain.card.UpgradePayment;
import ru.himukai.multiversecards.domain.card.UpgradeStat;
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

    private static final String SELECT_PLAYER = "SELECT name, cash, starter_set_claimed FROM players WHERE id = ?";

    private static final String SELECT_CARDS = """
            SELECT t.id AS template_id, t.name, t.universe, t.rarity,
                   t.base_hp, t.base_st, t.base_df, t.is_starter,
                   pc.hp_boost, pc.st_boost, pc.df_boost, pc.shards, pc.obtained_at
            FROM player_cards pc
            JOIN card_templates t ON t.id = pc.template_id
            WHERE pc.player_id = ?
            ORDER BY pc.obtained_at, t.id
            """;

    private static final String UPSERT_PLAYER = """
            INSERT INTO players (id, name, cash, starter_set_claimed) VALUES (?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, cash = EXCLUDED.cash
            """;

    private static final String CLAIM_STARTER_SET = """
            UPDATE players SET starter_set_claimed = TRUE
            WHERE id = ? AND starter_set_claimed = FALSE
            """;

    private static final String LOCK_PLAYER = "SELECT cash FROM players WHERE id = ? FOR UPDATE";
    private static final String LOCK_CARD_BOOSTS = """
            SELECT hp_boost, st_boost, df_boost, shards
            FROM player_cards WHERE player_id = ? AND template_id = ? FOR UPDATE
            """;
    private static final String DEBIT_GOLD = "UPDATE players SET cash = cash - ? WHERE id = ? AND cash >= ?";
    private static final String ADD_DUPLICATE_SHARDS = """
            UPDATE player_cards SET shards = shards + ?
            WHERE player_id = ? AND template_id = ? AND shards <= ?
            """;

    /** После апгрейда меняются усиления и осколки, а дата получения остаётся прежней */
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
            boolean starterSetClaimed;
            try (PreparedStatement ps = conn.prepareStatement(SELECT_PLAYER)) {
                ps.setLong(1, id.value());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    name = rs.getString("name");
                    cash = rs.getLong("cash");
                    starterSetClaimed = rs.getBoolean("starter_set_claimed");
                }
            }
            return Optional.of(new Player(id, name, cash, new CardCollection(loadCards(conn, id)), starterSetClaimed));
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

    @Override
    public boolean grantStarterSetOnce(PlayerId id, java.util.Collection<OwnedCard> cards) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int claimed;
                try (PreparedStatement ps = conn.prepareStatement(CLAIM_STARTER_SET)) {
                    ps.setLong(1, id.value());
                    claimed = ps.executeUpdate();
                }
                if (claimed == 0) {
                    conn.commit();
                    return false;
                }
                upsertCards(conn, id, cards);
                conn.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось выдать стартовый набор игроку " + id.value(), e);
        }
    }

    @Override
    public boolean upgradeCard(PlayerId playerId, TemplateId cardId, UpgradeStat stat, UpgradePayment payment,
                               int expectedLevel, int maxLevel, long goldCost, int shardCost) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(cardId, "cardId");
        Objects.requireNonNull(stat, "stat");
        Objects.requireNonNull(payment, "payment");
        if (expectedLevel < 0 || maxLevel < 0 || goldCost < 0 || shardCost < 0) {
            throw new IllegalArgumentException("Уровень и стоимость усиления не могут быть отрицательными");
        }

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                long cash;
                try (PreparedStatement ps = conn.prepareStatement(LOCK_PLAYER)) {
                    ps.setLong(1, playerId.value());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            conn.commit();
                            return false;
                        }
                        cash = rs.getLong("cash");
                    }
                }

                int hp;
                int st;
                int df;
                int shards;
                try (PreparedStatement ps = conn.prepareStatement(LOCK_CARD_BOOSTS)) {
                    ps.setLong(1, playerId.value());
                    ps.setString(2, cardId.value());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            conn.commit();
                            return false;
                        }
                        hp = rs.getInt("hp_boost");
                        st = rs.getInt("st_boost");
                        df = rs.getInt("df_boost");
                        shards = rs.getInt("shards");
                    }
                }
                int level = Math.addExact(Math.addExact(hp, st), df);
                if (level != expectedLevel || level >= maxLevel
                        || (payment == UpgradePayment.GOLD && cash < goldCost)
                        || (payment == UpgradePayment.SHARDS && shards < shardCost)) {
                    conn.commit();
                    return false;
                }

                if (payment == UpgradePayment.GOLD) {
                    try (PreparedStatement ps = conn.prepareStatement(DEBIT_GOLD)) {
                        ps.setLong(1, goldCost);
                        ps.setLong(2, playerId.value());
                        ps.setLong(3, goldCost);
                        if (ps.executeUpdate() != 1) {
                            conn.rollback();
                            return false;
                        }
                    }
                }

                String statColumn = switch (stat) {
                    case HP -> "hp_boost";
                    case ST -> "st_boost";
                    case DF -> "df_boost";
                };
                String sql = payment == UpgradePayment.SHARDS
                        ? "UPDATE player_cards SET " + statColumn + " = " + statColumn
                            + " + 1, shards = shards - ? WHERE player_id = ? AND template_id = ?"
                        : "UPDATE player_cards SET " + statColumn + " = " + statColumn
                            + " + 1 WHERE player_id = ? AND template_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    int parameter = 1;
                    if (payment == UpgradePayment.SHARDS) {
                        ps.setInt(parameter++, shardCost);
                    }
                    ps.setLong(parameter++, playerId.value());
                    ps.setString(parameter, cardId.value());
                    if (ps.executeUpdate() != 1) {
                        conn.rollback();
                        return false;
                    }
                }

                conn.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось усилить карточку " + cardId.value()
                    + " игрока " + playerId.value(), e);
        }
    }

    @Override
    public boolean convertDuplicateToShards(PlayerId playerId, TemplateId cardId, int shardAmount) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(cardId, "cardId");
        if (shardAmount < 0) {
            throw new IllegalArgumentException("Число осколков не может быть отрицательным");
        }
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(ADD_DUPLICATE_SHARDS)) {
            ps.setInt(1, shardAmount);
            ps.setLong(2, playerId.value());
            ps.setString(3, cardId.value());
            ps.setInt(4, Integer.MAX_VALUE - shardAmount);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось конвертировать дубликат карточки "
                    + cardId.value() + " в осколки", e);
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
                            new Stats(rs.getInt("base_hp"), rs.getInt("base_st"), rs.getInt("base_df")),
                            rs.getBoolean("is_starter")
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
            ps.setBoolean(4, player.starterSetClaimed());
            ps.executeUpdate();
        }
    }

    private static void upsertCards(Connection conn, Player player) throws SQLException {
        upsertCards(conn, player.id(), player.cards().all());
    }

    private static void upsertCards(Connection conn, PlayerId id,
                                    java.util.Collection<OwnedCard> cards) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPSERT_CARD)) {
            for (OwnedCard card : cards) {
                ps.setLong(1, id.value());
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
