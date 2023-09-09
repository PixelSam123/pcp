package io.github.pixelsam123.pcp.challenge.submission.vote;

import io.github.pixelsam123.pcp.user.UserBriefDto;
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
public class ChallengeSubmissionVoteRepository {
    private final DataSource dataSource;

    public ChallengeSubmissionVoteRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Uni<Long> countByChallengeSubmissionIdAndUserId(
        long challengeSubmissionId,
        long userId
    ) {
        Supplier<Long> dbOperation = Unchecked.supplier(() -> {
            try (
                Connection c = dataSource.getConnection();
                PreparedStatement statement = c.prepareStatement(
                    "SELECT COUNT(*) FROM challenge_submission_vote "
                        + "WHERE challenge_submission_id = ? AND user_id = ?"
                )
            ) {
                statement.setLong(1, challengeSubmissionId);
                statement.setLong(2, userId);

                ResultSet res = statement.executeQuery();
                res.next();

                return res.getLong(1);
            }
        });

        return Uni.createFrom().item(dbOperation).runSubscriptionOn(getDefaultWorkerPool());
    }

    public Uni<Optional<Long>> findUserIdById(long id) {
        Supplier<Optional<Long>> dbOperation = Unchecked.supplier(() -> {
            try (
                Connection c = dataSource.getConnection();
                PreparedStatement statement = c.prepareStatement(
                    "SELECT user_id FROM challenge_submission_vote WHERE id = ?"
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

        return Uni.createFrom().item(dbOperation).runSubscriptionOn(getDefaultWorkerPool());
    }

    public Uni<Optional<Boolean>> findIsUpvoteByChallengeSubmissionIdAndUserName(
        long challengeSubmissionId,
        String userName
    ) {
        Supplier<Optional<Boolean>> dbOperation = Unchecked.supplier(() -> {
            try (
                Connection c = dataSource.getConnection();
                PreparedStatement statement = c.prepareStatement(
                    "SELECT csv.is_upvote FROM challenge_submission_vote csv "
                        + "JOIN user u on csv.user_id = u.id "
                        + "WHERE csv.challenge_submission_id = ? AND u.name = ?"
                )
            ) {
                statement.setLong(1, challengeSubmissionId);
                statement.setString(2, userName);

                ResultSet res = statement.executeQuery();
                if (!res.next()) {
                    return Optional.empty();
                }

                return Optional.of(res.getBoolean("csv.is_upvote"));
            }
        });

        return Uni.createFrom().item(dbOperation).runSubscriptionOn(getDefaultWorkerPool());
    }

    public Uni<List<ChallengeSubmissionVoteDto>> listByChallengeSubmissionId(
        long challengeSubmissionId
    ) {
        Supplier<List<ChallengeSubmissionVoteDto>> dbOperation = Unchecked.supplier(() -> {
            try (
                Connection c = dataSource.getConnection();
                PreparedStatement statement = c.prepareStatement(
                    "SELECT "
                        + "csv.id,"
                        + "csv.is_upvote,"
                        + "u.id,"
                        + "u.name,"
                        + "u.points "
                        + "FROM challenge_submission_vote csv JOIN user u ON csv.user_id = u.id "
                        + "WHERE challenge_submission_id = ?"
                )
            ) {
                statement.setLong(1, challengeSubmissionId);

                List<ChallengeSubmissionVoteDto> list = new ArrayList<>();

                ResultSet res = statement.executeQuery();
                while (res.next()) {
                    list.add(new ChallengeSubmissionVoteDto(
                        res.getLong("csv.id"),
                        res.getBoolean("csv.is_upvote"),
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

        return Uni.createFrom().item(dbOperation).runSubscriptionOn(getDefaultWorkerPool());
    }

    public Uni<Void> persist(
        ChallengeSubmissionVoteCreateDto challengeSubmissionVote,
        long userId
    ) {
        Supplier<Void> dbOperation = Unchecked.supplier(() -> {
            try (
                Connection c = dataSource.getConnection();
                PreparedStatement statement = c.prepareStatement(
                    "INSERT INTO challenge_submission_vote "
                        + "(is_upvote, user_id, challenge_submission_id) "
                        + "VALUES (?, ?, ?)"
                )
            ) {
                statement.setBoolean(1, challengeSubmissionVote.isUpvote());
                statement.setLong(2, userId);
                statement.setLong(3, challengeSubmissionVote.submissionId());

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

    public Uni<Void> deleteById(Long id) {
        Supplier<Void> dbOperation = Unchecked.supplier(() -> {
            try (
                Connection c = dataSource.getConnection();
                PreparedStatement statement = c.prepareStatement(
                    "DELETE FROM challenge_submission_vote WHERE id = ?"
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

        return Uni.createFrom().item(dbOperation).runSubscriptionOn(getDefaultWorkerPool());
    }
}
