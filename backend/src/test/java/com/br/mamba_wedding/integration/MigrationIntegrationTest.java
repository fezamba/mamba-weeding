package com.br.mamba_wedding.integration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class MigrationIntegrationTest {

    @Container
    @SuppressWarnings("resource") // O Testcontainers encerra o container ao finalizar esta classe.
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("mamba_migrations_test")
            .withUsername("mamba")
            .withPassword("mamba");

    @Test
    void migrations_ShouldCreateAndValidateAnEmptySchema() throws SQLException {
        String schema = "empty_schema";
        createSchema(schema);

        Flyway flyway = flywayFor(schema, false);
        MigrateResult firstRun = flyway.migrate();
        MigrateResult secondRun = flyway.migrate();

        assertThat(firstRun.migrationsExecuted).isEqualTo(4);
        assertThat(secondRun.migrationsExecuted).isZero();
        assertThat(flyway.validateWithResult().validationSuccessful).isTrue();

        try (Connection connection = connectionFor(schema)) {
            assertThat(tableExists(connection, "guests")).isTrue();
            assertThat(tableExists(connection, "gifts")).isTrue();
            assertThat(tableExists(connection, "gift_transactions")).isTrue();
            assertThat(tableExists(connection, "events")).isTrue();
            assertThat(tableExists(connection, "event_invitations")).isTrue();
            assertThat(columnExists(connection, "gift_transactions", "guest_id")).isTrue();
            assertThat(columnExists(connection, "gifts", "event_id")).isTrue();
            assertThat(columnExists(connection, "gift_transactions", "guest_name")).isFalse();
            assertThat(columnExists(connection, "guests", "rsvp_status")).isFalse();
            assertThat(columnExists(connection, "guests", "rsvp_by")).isFalse();
            assertThat(columnExists(connection, "guests", "notes")).isFalse();
            assertThat(queryForLong(connection, "SELECT count(*) FROM events")).isEqualTo(2L);
            assertThat(queryForString(connection,
                    "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank DESC LIMIT 1"))
                    .isEqualTo("4");
        }
    }

    @Test
    void migrations_ShouldUpgradeLegacySchemaAndPreserveReservationOwner() throws SQLException {
        String schema = "legacy_schema";
        createLegacySchema(schema);

        Flyway flyway = flywayFor(schema, true);
        MigrateResult result = flyway.migrate();

        assertThat(result.migrationsExecuted).isEqualTo(4);
        assertThat(flyway.validateWithResult().validationSuccessful).isTrue();

        try (Connection connection = connectionFor(schema)) {
            assertThat(columnExists(connection, "gift_transactions", "guest_id")).isTrue();
            assertThat(columnExists(connection, "gift_transactions", "guest_name")).isFalse();
            assertThat(queryForLong(connection,
                    "SELECT guest_id FROM gift_transactions WHERE id = 100"))
                    .isEqualTo(10L);
            assertThat(queryForLong(connection,
                    "SELECT count(*) FROM gift_transactions WHERE id = 100 AND status = 'RESERVED'"))
                    .isEqualTo(1L);
            assertThat(queryForLong(connection, """
                    SELECT count(*)
                    FROM gifts gift
                    JOIN events event ON event.id = gift.event_id
                    WHERE gift.id = 20
                      AND event.type = 'WEDDING'
                    """))
                    .isEqualTo(1L);
            assertThat(queryForLong(connection, """
                    SELECT count(*)
                    FROM event_invitations invitation
                    JOIN events event ON event.id = invitation.event_id
                    WHERE invitation.guest_id = 10
                      AND event.type = 'WEDDING'
                      AND invitation.rsvp_status = 'CONFIRMED'
                      AND invitation.responded_at = TIMESTAMP '2026-07-20 14:30:00'
                      AND invitation.notes = 'Sem glúten'
                    """))
                    .isEqualTo(1L);
            assertThat(queryForLong(connection, """
                    SELECT count(*)
                    FROM event_invitations invitation
                    JOIN events event ON event.id = invitation.event_id
                    WHERE invitation.guest_id = 10
                      AND event.type = 'BRIDAL_SHOWER'
                      AND invitation.rsvp_status = 'PENDING'
                      AND invitation.responded_at IS NULL
                      AND invitation.notes IS NULL
                    """))
                    .isEqualTo(1L);
            assertThat(columnExists(connection, "guests", "rsvp_status")).isFalse();
            assertThat(columnExists(connection, "guests", "rsvp_by")).isFalse();
            assertThat(columnExists(connection, "guests", "notes")).isFalse();
            assertThat(queryForLong(connection,
                    "SELECT count(*) FROM flyway_schema_history WHERE type = 'BASELINE' AND version = '0'"))
                    .isEqualTo(1L);
        }
    }

    private Flyway flywayFor(String schema, boolean baselineOnMigrate) {
        return Flyway.configure()
                .dataSource(jdbcUrlFor(schema), POSTGRES.getUsername(), POSTGRES.getPassword())
                .defaultSchema(schema)
                .schemas(schema)
                .locations("classpath:db/migration")
                .baselineOnMigrate(baselineOnMigrate)
                .baselineVersion("0")
                .cleanDisabled(true)
                .load();
    }

    private void createSchema(String schema) throws SQLException {
        try (Connection connection = baseConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);
        }
    }

    private void createLegacySchema(String schema) throws SQLException {
        createSchema(schema);

        try (Connection connection = connectionFor(schema); Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE guests (
                        id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                        full_name VARCHAR(255) NOT NULL,
                        rsvp_code VARCHAR(32) NOT NULL,
                        rsvp_status VARCHAR(16) NOT NULL,
                        rsvp_by TIMESTAMP,
                        side VARCHAR(10) NOT NULL,
                        email VARCHAR(120) NOT NULL,
                        phone VARCHAR(30) NOT NULL,
                        notes VARCHAR(255)
                    )
                    """);
            statement.execute("""
                    CREATE TABLE gifts (
                        id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                        version BIGINT,
                        name VARCHAR(120) NOT NULL,
                        description VARCHAR(500),
                        value NUMERIC(10, 2) NOT NULL,
                        total_quotas INTEGER NOT NULL,
                        image_url VARCHAR(255),
                        purchase_link VARCHAR(255)
                    )
                    """);
            statement.execute("""
                    CREATE TABLE gift_transactions (
                        id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                        gift_id BIGINT NOT NULL REFERENCES gifts(id),
                        guest_name VARCHAR(120) NOT NULL,
                        number_quotas INTEGER NOT NULL,
                        status VARCHAR(16) NOT NULL,
                        reserved_at TIMESTAMP,
                        reserved_until TIMESTAMP,
                        purchased_at TIMESTAMP
                    )
                    """);
            statement.execute("""
                    INSERT INTO guests
                        (id, full_name, rsvp_code, rsvp_status, rsvp_by, side, email, phone, notes)
                    VALUES
                        (10, 'Convidada Legada', 'LEG1234', 'CONFIRMED',
                         TIMESTAMP '2026-07-20 14:30:00', 'BRIDE',
                         'legada@example.com', '11999999999', 'Sem glúten')
                    """);
            statement.execute("""
                    INSERT INTO gifts
                        (id, version, name, value, total_quotas)
                    VALUES
                        (20, 0, 'Presente legado', 500.00, 5)
                    """);
            statement.execute("""
                    INSERT INTO gift_transactions
                        (id, gift_id, guest_name, number_quotas, status, reserved_at, reserved_until)
                    VALUES
                        (100, 20, '  convidada legada  ', 2, 'RESERVED', now(), now() + interval '6 hours')
                    """);
        }
    }

    private Connection baseConnection() throws SQLException {
        return DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private Connection connectionFor(String schema) throws SQLException {
        return DriverManager.getConnection(
                jdbcUrlFor(schema), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private String jdbcUrlFor(String schema) {
        String separator = POSTGRES.getJdbcUrl().contains("?") ? "&" : "?";
        return POSTGRES.getJdbcUrl() + separator + "currentSchema=" + schema;
    }

    private boolean tableExists(Connection connection, String table) throws SQLException {
        try (ResultSet result = connection.getMetaData().getTables(null, null, table, new String[]{"TABLE"})) {
            return result.next();
        }
    }

    private boolean columnExists(Connection connection, String table, String column) throws SQLException {
        try (ResultSet result = connection.getMetaData().getColumns(null, null, table, column)) {
            return result.next();
        }
    }

    private long queryForLong(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getLong(1);
        }
    }

    private String queryForString(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getString(1);
        }
    }
}
