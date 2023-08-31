package io.github.pixelsam123.pcp.challenge;

import io.github.pixelsam123.pcp.user.UserBriefDto;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
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

@ApplicationScoped
public class ChallengeRepository {
    private final DataSource dataSource;

    public ChallengeRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Uni<Long> countByName(String name) {
        Supplier<Long> dbOperation = Unchecked.supplier(() -> {
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

        return Uni
            .createFrom()
            .item(dbOperation)
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    public Uni<Long> countById(long id) {
        Supplier<Long> dbOperation = Unchecked.supplier(() -> {
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

        return Uni
            .createFrom()
            .item(dbOperation)
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    public Uni<Optional<Integer>> findTierById(long id) {
        Supplier<Optional<Integer>> dbOperation = Unchecked.supplier(() -> {
            try (
                Connection c = dataSource.getConnection();
                PreparedStatement statement = c.prepareStatement(
                    "SELECT tier FROM challenge WHERE id = ?"
                )
            ) {
                statement.setLong(1, id);

                ResultSet res = statement.executeQuery();
                if (!res.next()) {
                    return Optional.empty();
                }

                return Optional.of(res.getInt("tier"));
            }
        });

        return Uni
            .createFrom()
            .item(dbOperation)
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    public Uni<Optional<Long>> findIdByName(String name) {
        Supplier<Optional<Long>> dbOperation = Unchecked.supplier(() -> {
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

        return Uni
            .createFrom()
            .item(dbOperation)
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    public Uni<Optional<ChallengeDto>> findByNameDto(String name) {
        Supplier<Optional<ChallengeDto>> dbOperation = Unchecked.supplier(() -> {
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
                        + "FROM challenge c JOIN user u on c.user_id = u.id WHERE c.name = ?"
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

        return Uni
            .createFrom()
            .item(dbOperation)
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    public Uni<List<ChallengeBriefDto>> listAllBrief() {
        Supplier<List<ChallengeBriefDto>> dbOperation = Unchecked.supplier(() -> {
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
                        + "FROM challenge c JOIN user u on c.user_id = u.id"
                )
            ) {
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

        return Uni
            .createFrom()
            .item(dbOperation)
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    public Uni<Void> addCompletedCountById(long id) {
        Supplier<Void> dbOperation = Unchecked.supplier(() -> {
            try (
                Connection c = dataSource.getConnection();
                PreparedStatement statement = c.prepareStatement(
                    "UPDATE challenge SET completed_count = completed_count + 1 WHERE id = ?"
                )
            ) {
                statement.setLong(1, id);

                if (statement.executeUpdate() < 1) {
                    throw new InternalServerErrorException(
                        "Update error: updated row count is less than 1"
                    );
                }

                return null;
            }
        });

        return Uni
            .createFrom()
            .item(dbOperation)
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    public Uni<Void> persist(ChallengeCreateDto challenge, long userId) {
        Supplier<Void> dbOperation = Unchecked.supplier(() -> {
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
                    throw new InternalServerErrorException(
                        "Insertion error: inserted row count is less than 1"
                    );
                }

                return null;
            }
        });

        return Uni
            .createFrom()
            .item(dbOperation)
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }
}
