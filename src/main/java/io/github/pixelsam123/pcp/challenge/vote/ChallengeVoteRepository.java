package io.github.pixelsam123.pcp.challenge.vote;

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
public class ChallengeVoteRepository {
    private final DataSource dataSource;

    public ChallengeVoteRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Uni<Long> countByChallengeIdAndUserId(long challengeId, long userId) {
        return Utils.runInWorkerPool(() -> {
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
    }

    public Uni<Optional<Long>> findUserIdById(long id) {
        return Utils.runInWorkerPool(() -> {
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
    }

    public Uni<Optional<ChallengeVoteDto>> findByChallengeNameAndUserName(
        String challengeName,
        String userName
    ) {
        return Utils.runInWorkerPool(() -> {
            try (
                Connection c = dataSource.getConnection();
                PreparedStatement statement = c.prepareStatement(
                    "SELECT cv.id, cv.is_upvote, u.id, u.name, u.points FROM challenge_vote cv "
                        + "JOIN challenge c ON cv.challenge_id = c.id "
                        + "JOIN user u ON cv.user_id = u.id "
                        + "WHERE c.name = ? AND u.name = ?"
                )
            ) {
                statement.setString(1, challengeName);
                statement.setString(2, userName);

                ResultSet res = statement.executeQuery();
                if (!res.next()) {
                    return Optional.empty();
                }

                return Optional.of(new ChallengeVoteDto(
                    res.getLong("cv.id"),
                    res.getBoolean("cv.is_upvote"),
                    new UserBriefDto(
                        res.getLong("u.id"),
                        res.getString("u.name"),
                        res.getInt("u.points")
                    )
                ));
            }
        });
    }

    public Uni<List<ChallengeVoteDto>> listByChallengeId(long challengeId) {
        return Utils.runInWorkerPool(() -> {
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
    }

    public Uni<Void> persist(ChallengeVoteCreateDto challengeVote, long userId) {
        return Utils.runInWorkerPool(() -> {
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
                    "DELETE FROM challenge_vote WHERE id = ?"
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
