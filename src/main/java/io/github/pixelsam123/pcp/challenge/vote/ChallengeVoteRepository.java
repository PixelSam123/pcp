package io.github.pixelsam123.pcp.challenge.vote;

import io.github.pixelsam123.pcp.challenge.Challenge;
import io.github.pixelsam123.pcp.user.User;
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
public class ChallengeVoteRepository {
    private final DataSource dataSource;

    public ChallengeVoteRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Uni<Long> countByChallengeIdAndUserId(long challengeId, long userId) {
        Supplier<Long> dbOperation = Unchecked.supplier(() -> {
            try (
                Connection c = dataSource.getConnection();
                PreparedStatement statement = c.prepareStatement(
                    "SELECT COUNT(*) FROM challenge_vote WHERE challenge_id = ? AND user_id = ?"
                )
            ) {
                statement.setLong(1, challengeId);
                statement.setLong(2, userId);

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

    public Uni<Optional<ChallengeVote>> findById(long id) {
        Supplier<Optional<ChallengeVote>> dbOperation = Unchecked.supplier(() -> {
            try (
                Connection c = dataSource.getConnection();
                PreparedStatement statement = c.prepareStatement(
                    "SELECT * FROM challenge_vote cv "
                        + "JOIN user u on cv.user_id = u.id "
                        + "JOIN challenge c on cv.challenge_id = c.id "
                        + "JOIN user c_u on c.user_id = c_u.id "
                        + "WHERE cv.id = ?"
                )
            ) {
                statement.setLong(1, id);

                ResultSet res = statement.executeQuery();
                if (!res.next()) {
                    return Optional.empty();
                }

                return Optional.of(new ChallengeVote(
                    res.getLong("cv.id"),
                    res.getBoolean("cv.is_upvote"),
                    new User(
                        res.getLong("u.id"),
                        res.getString("u.name"),
                        res.getString("u.password_hash"),
                        res.getString("u.role"),
                        res.getInt("u.points")
                    ),
                    new Challenge(
                        res.getLong("c.id"),
                        res.getString("c.name"),
                        res.getString("c.description"),
                        res.getString("c.initial_code"),
                        res.getString("c.test_case"),
                        res.getInt("c.tier"),
                        res.getInt("c.completed_count"),
                        new User(
                            res.getLong("c_u.id"),
                            res.getString("c_u.name"),
                            res.getString("c_u.password_hash"),
                            res.getString("c_u.role"),
                            res.getInt("c_u.points")
                        )
                    )
                ));
            }
        });

        return Uni
            .createFrom()
            .item(dbOperation)
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    public Uni<List<ChallengeVoteDto>> listByChallengeId(long challengeId) {
        Supplier<List<ChallengeVoteDto>> dbOperation = Unchecked.supplier(() -> {
            try (
                Connection c = dataSource.getConnection();
                PreparedStatement statement = c.prepareStatement(
                    "SELECT "
                        + "cv.id, "
                        + "cv.is_upvote, "
                        + "u.id, "
                        + "u.name, "
                        + "u.points "
                        + "FROM challenge_vote cv JOIN user u on cv.user_id = u.id "
                        + "WHERE cv.challenge_id = ?"
                )
            ) {
                statement.setLong(1, challengeId);

                List<ChallengeVoteDto> list = new ArrayList<>();

                ResultSet res = statement.executeQuery();
                while (res.next()) {
                    list.add(new ChallengeVoteDto(
                        res.getLong("cv.id"),
                        res.getBoolean("cv.is_upvote"),
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

    public Uni<Void> persist(ChallengeVoteCreateDto challengeVote, long userId) {
        Supplier<Void> dbOperation = Unchecked.supplier(() -> {
            try (
                Connection c = dataSource.getConnection();
                PreparedStatement statement = c.prepareStatement(
                    "INSERT INTO challenge_vote (is_upvote, user_id, challenge_id) "
                        + "VALUES (?, ?, ?)"
                )
            ) {
                statement.setBoolean(1, challengeVote.isUpvote());
                statement.setLong(2, userId);
                statement.setLong(3, challengeVote.challengeId());

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

    public Uni<Void> deleteById(long id) {
        Supplier<Void> dbOperation = Unchecked.supplier(() -> {
            try (
                Connection c = dataSource.getConnection();
                PreparedStatement statement = c.prepareStatement(
                    "DELETE FROM challenge_vote WHERE id = ?"
                )
            ) {
                statement.setLong(1, id);

                if (statement.executeUpdate() < 1) {
                    throw new InternalServerErrorException(
                        "Deletion error: deleted row count is less than 1"
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
