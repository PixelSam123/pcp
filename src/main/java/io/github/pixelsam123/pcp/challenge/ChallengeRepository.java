package io.github.pixelsam123.pcp.challenge;

import io.github.pixelsam123.pcp.common.Utils;
import io.github.pixelsam123.pcp.user.UserBriefDto;
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
public class ChallengeRepository {
    private final DataSource dataSource;

    public ChallengeRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Uni<Long> countByName(String name) {
        return Utils.runInWorkerPool(() -> {
            try (
                Connection c = dataSource.getConnection();
                PreparedStatement statement = c.prepareStatement(
                    "SELECT COUNT(*) FROM challenge WHERE name = ?"
                )
            ) {
                statement.setString(1, name);

                ResultSet res = statement.executeQuery();
                res.next();

                return res.getLong(1);
            }
        });
    }

    public Uni<Long> countById(long id) {
        return Utils.runInWorkerPool(() -> {
            try (
                Connection c = dataSource.getConnection();
                PreparedStatement statement = c.prepareStatement(
                    "SELECT COUNT(*) FROM challenge WHERE id = ?"
                )
            ) {
                statement.setLong(1, id);

                ResultSet res = statement.executeQuery();
                res.next();

                return res.getLong(1);
            }
        });
    }

    public Uni<Optional<ChallengeVerifierView>> findVerifierById(long id) {
        return Utils.runInWorkerPool(() -> {
            try (
                Connection c = dataSource.getConnection();
                PreparedStatement statement = c.prepareStatement(
                    "SELECT tier, test_case FROM challenge WHERE id = ?"
                )
            ) {
                statement.setLong(1, id);

                ResultSet res = statement.executeQuery();
                if (!res.next()) {
                    return Optional.empty();
                }

                return Optional.of(new ChallengeVerifierView(
                    res.getInt("tier"),
                    res.getString("test_case")
                ));
            }
        });
    }

    public Uni<Optional<Long>> findIdByName(String name) {
        return Utils.runInWorkerPool(() -> {
            try (
                Connection c = dataSource.getConnection();
                PreparedStatement statement = c.prepareStatement(
                    "SELECT id FROM challenge WHERE name = ?"
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

    public Uni<Optional<Long>> findUserIdById(long id) {
        return Utils.runInWorkerPool(() -> {
            try (
                Connection c = dataSource.getConnection();
                PreparedStatement statement = c.prepareStatement(
                    "SELECT user_id FROM challenge WHERE id = ?"
                )
            ) {
                statement.setLong(1, id);

                ResultSet res = statement.executeQuery();
                if (!res.next()) {
                    return Optional.empty();
                }

                return Optional.of(res.getLong("user_id"));
            }
        });
    }

    public Uni<Optional<ChallengeDto>> findDtoByName(String name) {
        return Utils.runInWorkerPool(() -> {
            try (
                Connection c = dataSource.getConnection();
                PreparedStatement statement = c.prepareStatement(
                    "SELECT "
                        + "c.id, "
                        + "c.name, "
                        + "c.tier, "
                        + "c.completed_count, "
                        + "u.id, "
                        + "u.name, "
                        + "u.points, "
                        + "c.description, "
                        + "c.initial_code "
                        + "FROM challenge c JOIN user u ON c.user_id = u.id WHERE c.name = ?"
                )
            ) {
                statement.setString(1, name);

                ResultSet res = statement.executeQuery();
                if (!res.next()) {
                    return Optional.empty();
                }

                return Optional.of(new ChallengeDto(
                    res.getLong("c.id"),
                    res.getString("c.name"),
                    res.getInt("c.tier"),
                    res.getInt("c.completed_count"),
                    new UserBriefDto(
                        res.getLong("u.id"),
                        res.getString("u.name"),
                        res.getInt("u.points")
                    ),
                    res.getString("c.description"),
                    res.getString("c.initial_code")
                ));
            }
        });
    }

    public Uni<List<ChallengeBriefDto>> list(
        List<Integer> tiers,
        String username,
        ChallengeSort sort
    ) {
        if (tiers.isEmpty()) {
            return Uni.createFrom().item(List::of);
        }

        return Utils.runInWorkerPool(() -> {
            String questionMarks = "?, ".repeat(tiers.size());

            try (
                Connection c = dataSource.getConnection();
                PreparedStatement statement = c.prepareStatement(
                    "SELECT "
                        + "c.id, "
                        + "c.name, "
                        + "c.tier, "
                        + "c.completed_count, "
                        + "u.id, "
                        + "u.name, "
                        + "u.points "
                        + "FROM challenge c JOIN user u ON c.user_id = u.id "
                        + "WHERE ? IS NULL OR u.name = ? AND c.tier IN ("
                        + questionMarks.substring(0, questionMarks.length() - ", ".length())
                        + ") "
                        + sort.sql
                )
            ) {
                statement.setString(1, username);
                statement.setString(2, username);

                int placeholderIdx = 3;
                for (int tier : tiers) {
                    statement.setInt(placeholderIdx, tier);
                    placeholderIdx++;
                }

                List<ChallengeBriefDto> list = new ArrayList<>();

                ResultSet res = statement.executeQuery();
                while (res.next()) {
                    list.add(new ChallengeBriefDto(
                        res.getLong("c.id"),
                        res.getString("c.name"),
                        res.getInt("c.tier"),
                        res.getInt("c.completed_count"),
                        new UserBriefDto(
                            res.getLong("u.id"),
                            res.getString("u.name"),
                            res.getInt("u.points")
                        )
                    ));
                }

                return list;
            }
        });
    }

    public Uni<Void> addCompletedCountById(long id) {
        return Utils.runInWorkerPool(() -> {
            try (
                Connection c = dataSource.getConnection();
                PreparedStatement statement = c.prepareStatement(
                    "UPDATE challenge SET completed_count = completed_count + 1 WHERE id = ?"
                )
            ) {
                statement.setLong(1, id);

                if (statement.executeUpdate() < 1) {
                    throw new RuntimeException("Update error: updated row count is less than 1");
                }

                return null;
            }
        });
    }

    public Uni<Void> persist(ChallengeCreateDto challenge, long userId) {
        return Utils.runInWorkerPool(() -> {
            try (
                Connection c = dataSource.getConnection();
                PreparedStatement statement = c.prepareStatement(
                    "INSERT INTO challenge "
                        + "(name, description, initial_code, test_case, tier, user_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?)"
                )
            ) {
                statement.setString(1, challenge.name());
                statement.setString(2, challenge.description());
                statement.setString(3, challenge.initialCode());
                statement.setString(4, challenge.testCase());
                statement.setInt(5, challenge.tier());
                statement.setLong(6, userId);

                if (statement.executeUpdate() < 1) {
                    throw new RuntimeException("Insert error: inserted row count is less than 1");
                }

                return null;
            }
        });
    }

    public Uni<Void> deleteById(long id) {
        return Utils.runInWorkerPool(() -> {
            try (
                Connection c = dataSource.getConnection();
                PreparedStatement statement = c.prepareStatement(
                    "DELETE FROM challenge WHERE id = ?"
                )
            ) {
                statement.setLong(1, id);

                if (statement.executeUpdate() < 1) {
                    throw new RuntimeException("Delete error: deleted row count is less than 1");
                }

                return null;
            }
        });
    }
}
