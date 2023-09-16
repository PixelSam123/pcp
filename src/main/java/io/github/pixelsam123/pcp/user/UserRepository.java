package io.github.pixelsam123.pcp.user;

import io.github.pixelsam123.pcp.common.Utils;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class UserRepository {
    private final DataSource dataSource;

    public UserRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Uni<Long> countByName(String name) {
        return Utils.runInWorkerPool(() -> {
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
    }

    public Uni<Optional<Long>> findIdByName(String name) {
        return Utils.runInWorkerPool(() -> {
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
    }

    public Uni<Optional<UserCredentialsView>> findCredentialsByName(String name) {
        return Utils.runInWorkerPool(() -> {
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
    }

    public Uni<Optional<UserBriefDto>> findBriefByName(String name) {
        return Utils.runInWorkerPool(() -> {
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
    }

    public Uni<List<UserBriefDto>> listAllBrief() {
        return Utils.runInWorkerPool(() -> {
            try (
                Connection c = dataSource.getConnection();
                PreparedStatement statement = c.prepareStatement(
                    "SELECT id, name, points FROM user ORDER BY points DESC"
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
    }

    public Uni<Void> addPointsById(long id, int points) {
        return Utils.runInWorkerPool(() -> {
            try (
                Connection c = dataSource.getConnection();
                PreparedStatement statement = c.prepareStatement(
                    "UPDATE user SET points = points + ? WHERE id = ?"
                )
            ) {
                statement.setInt(1, points);
                statement.setLong(2, id);

                if (statement.executeUpdate() < 1) {
                    throw new RuntimeException("Update error: updated row count is less than 1");
                }

                return null;
            }
        });
    }

    public Uni<Void> persist(String name, String passwordHash) {
        return Utils.runInWorkerPool(() -> {
            try (
                Connection c = dataSource.getConnection();
                PreparedStatement statement = c.prepareStatement(
                    "INSERT INTO user (name, password_hash) VALUES (?, ?)"
                )
            ) {
                statement.setString(1, name);
                statement.setString(2, passwordHash);

                if (statement.executeUpdate() < 1) {
                    throw new RuntimeException("Insert error: inserted row count is less than 1");
                }

                return null;
            }
        });
    }
}
