package io.github.pixelsam123.pcp.challenge.vote;

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

    public Uni<Optional<Long>> findUserIdById(long id) {
        Supplier<Optional<Long>> dbOperation = Unchecked.supplier(() -> {
            try (
                Connection c = dataSource.getConnection();
                PreparedStatement statement = c.prepareStatement(
                    "SELECT user_id FROM challenge_vote WHERE id = ?"
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
                        + "FROM challenge_vote cv JOIN user u ON cv.user_id = u.id "
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
