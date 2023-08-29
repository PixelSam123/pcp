package io.github.pixelsam123.pcp.challenge;

import io.github.pixelsam123.pcp.user.User;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.unchecked.Unchecked;
import jakarta.enterprise.context.ApplicationScoped;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

    public Uni<Optional<Challenge>> findById(long id) {
        Supplier<Optional<Challenge>> dbOperation = Unchecked.supplier(() -> {
            try (
                Connection c = dataSource.getConnection();
                PreparedStatement statement = c.prepareStatement(
                    "SELECT * FROM challenge c JOIN user u on u.id = c.user_id WHERE c.id = ?"
                )
            ) {
                statement.setLong(1, id);

                ResultSet res = statement.executeQuery();
                if (!res.next()) {
                    return Optional.empty();
                }

                return Optional.of(new Challenge(
                    res.getLong("c.id"),
                    res.getString("c.name"),
                    res.getString("c.description"),
                    res.getString("c.initial_code"),
                    res.getString("c.test_case"),
                    res.getInt("c.tier"),
                    res.getInt("c.completed_count"),
                    new User(
                        res.getLong("u.id"),
                        res.getString("u.name"),
                        res.getString("u.password_hash"),
                        res.getString("u.role"),
                        res.getInt("u.points")
                    )
                ));
            }
        });

        return Uni
            .createFrom()
            .item(dbOperation)
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    public Uni<Optional<Challenge>> findByName(String name) {
        Supplier<Optional<Challenge>> dbOperation = Unchecked.supplier(() -> {
            try (
                Connection c = dataSource.getConnection();
                PreparedStatement statement = c.prepareStatement(
                    "SELECT * FROM challenge c JOIN user u on u.id = c.user_id WHERE c.name = ?"
                )
            ) {
                statement.setString(1, name);

                ResultSet res = statement.executeQuery();
                if (!res.next()) {
                    return Optional.empty();
                }

                return Optional.of(new Challenge(
                    res.getLong("c.id"),
                    res.getString("c.name"),
                    res.getString("c.description"),
                    res.getString("c.initial_code"),
                    res.getString("c.test_case"),
                    res.getInt("c.tier"),
                    res.getInt("c.completed_count"),
                    new User(
                        res.getLong("u.id"),
                        res.getString("u.name"),
                        res.getString("u.password_hash"),
                        res.getString("u.role"),
                        res.getInt("u.points")
                    )
                ));
            }
        });

        return Uni
            .createFrom()
            .item(dbOperation)
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    public Uni<Optional<ChallengeDto>> asyncFindByNameDto(String name) {
        return Uni
            .createFrom()
            .item(() -> find("name", name).project(ChallengeDto.class).firstResultOptional())
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    public Uni<List<ChallengeBriefDto>> asyncListAllBrief() {
        return Uni
            .createFrom()
            .item(() -> findAll().project(ChallengeBriefDto.class).list())
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    public Uni<Void> asyncAddCompletedCount(Challenge challenge) {
        return Uni
            .createFrom()
            .item(() -> {
                challenge.setCompletedCount(challenge.getCompletedCount() + 1);

                return null;
            })
            .replaceWithVoid()
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    public Uni<Void> asyncPersist(Challenge challenge) {
        return Uni
            .createFrom()
            .item(() -> {
                persist(challenge);

                return null;
            })
            .replaceWithVoid()
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }
}
