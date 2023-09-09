package io.github.pixelsam123.pcp.user;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.InternalServerErrorException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static io.smallrye.mutiny.infrastructure.Infrastructure.getDefaultWorkerPool;

@ApplicationScoped
public class UserRepository {
    private final DataSource dataSource;

    public UserRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Uni<Long> countByName(String name) {
        Supplier<Long> dbOperation = Unchecked.supplier(() -> {
            try (
                Connection c = dataSource.getConnection();
                PreparedStatement statement = c.prepareStatement(
                    "SELECT COUNT(*) FROM user WHERE name = ?"
                )
            ) {
                statement.setString(1, name);

                ResultSet res = statement.executeQuery();
                res.next();

                return res.getLong(1);
            }
        });

        return Uni.createFrom().item(dbOperation).runSubscriptionOn(getDefaultWorkerPool());
    }

    public Uni<Optional<Long>> findIdByName(String name) {
        Supplier<Optional<Long>> dbOperation = Unchecked.supplier(() -> {
            try (
                Connection c = dataSource.getConnection();
                PreparedStatement statement = c.prepareStatement(
                    "SELECT id FROM user WHERE name = ?"
                )
            ) {
                statement.setString(1, name);

                ResultSet res = statement.executeQuery();
                if (!res.next()) {
                    return Optional.empty();
                }

                return Optional.of(res.getLong("id"));
            }
        });

        return Uni.createFrom().item(dbOperation).runSubscriptionOn(getDefaultWorkerPool());
    }

    public Uni<Optional<UserCredentialsView>> findCredentialsByName(String name) {
        Supplier<Optional<UserCredentialsView>> dbOperation = Unchecked.supplier(() -> {
            try (
                Connection c = dataSource.getConnection();
                PreparedStatement statement = c.prepareStatement(
                    "SELECT id, password_hash FROM user WHERE name = ?"
                )
            ) {
                statement.setString(1, name);

                ResultSet res = statement.executeQuery();
                if (!res.next()) {
                    return Optional.empty();
                }

                return Optional.of(new UserCredentialsView(
                    res.getLong("id"),
                    res.getString("password_hash")
                ));
            }
        });

        return Uni.createFrom().item(dbOperation).runSubscriptionOn(getDefaultWorkerPool());
    }

    public Uni<Optional<UserBriefDto>> findBriefByName(String name) {
        Supplier<Optional<UserBriefDto>> dbOperation = Unchecked.supplier(() -> {
            try (
                Connection c = dataSource.getConnection();
                PreparedStatement statement = c.prepareStatement(
                    "SELECT id, name, points FROM user WHERE name = ?"
                )
            ) {
                statement.setString(1, name);

                ResultSet res = statement.executeQuery();
                if (!res.next()) {
                    return Optional.empty();
                }

                return Optional.of(new UserBriefDto(
                    res.getLong("id"),
                    res.getString("name"),
                    res.getInt("points")
                ));
            }
        });

        return Uni.createFrom().item(dbOperation).runSubscriptionOn(getDefaultWorkerPool());
    }

    public Uni<List<UserBriefDto>> listAllBrief() {
        Supplier<List<UserBriefDto>> dbOperation = Unchecked.supplier(() -> {
            try (
                Connection c = dataSource.getConnection();
                PreparedStatement statement = c.prepareStatement(
                    "SELECT id, name, points FROM user"
                )
            ) {
                List<UserBriefDto> list = new ArrayList<>();

                ResultSet res = statement.executeQuery();
                while (res.next()) {
                    list.add(new UserBriefDto(
                        res.getLong("id"),
                        res.getString("name"),
                        res.getInt("points")
                    ));
                }

                return list;
            }
        });

        return Uni.createFrom().item(dbOperation).runSubscriptionOn(getDefaultWorkerPool());
    }

    public Uni<Void> addPointsById(long id, int points) {
        Supplier<Void> dbOperation = Unchecked.supplier(() -> {
            try (
                Connection c = dataSource.getConnection();
                PreparedStatement statement = c.prepareStatement(
                    "UPDATE user SET points = points + ? WHERE id = ?"
                )
            ) {
                statement.setInt(1, points);
                statement.setLong(2, id);

                if (statement.executeUpdate() < 1) {
                    throw new InternalServerErrorException(
                        "Update error: updated row count is less than 1"
                    );
                }

                return null;
            }
        });

        return Uni.createFrom().item(dbOperation).runSubscriptionOn(getDefaultWorkerPool());
    }

    public Uni<Void> persist(String name, String passwordHash) {
        Supplier<Void> dbOperation = Unchecked.supplier(() -> {
            try (
                Connection c = dataSource.getConnection();
                PreparedStatement statement = c.prepareStatement(
                    "INSERT INTO user (name, password_hash) VALUES (?, ?)"
                )
            ) {
                statement.setString(1, name);
                statement.setString(2, passwordHash);

                if (statement.executeUpdate() < 1) {
                    throw new InternalServerErrorException(
                        "Insertion error: inserted row count is less than 1"
                    );
                }

                return null;
            }
        });

        return Uni.createFrom().item(dbOperation).runSubscriptionOn(getDefaultWorkerPool());
    }
}
