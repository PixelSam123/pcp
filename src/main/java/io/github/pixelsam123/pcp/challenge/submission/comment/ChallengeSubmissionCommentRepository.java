package io.github.pixelsam123.pcp.challenge.submission.comment;

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
import java.util.function.Supplier;

import static io.smallrye.mutiny.infrastructure.Infrastructure.getDefaultWorkerPool;

@ApplicationScoped
public class ChallengeSubmissionCommentRepository {
    private final DataSource dataSource;

    public ChallengeSubmissionCommentRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Uni<List<ChallengeSubmissionCommentDto>> listByChallengeSubmissionId(long challengeSubmissionId) {
        Supplier<List<ChallengeSubmissionCommentDto>> dbOperation = Unchecked.supplier(() -> {
            try (
                Connection c = dataSource.getConnection();
                PreparedStatement statement = c.prepareStatement(
                    "SELECT "
                        + "csc.id, "
                        + "csc.content, "
                        + "u.id, "
                        + "u.name, "
                        + "u.points "
                        + "FROM challenge_submission_comment csc JOIN user u ON csc.user_id = u.id "
                        + "WHERE challenge_submission_id = ?"
                )
            ) {
                statement.setLong(1, challengeSubmissionId);

                List<ChallengeSubmissionCommentDto> list = new ArrayList<>();

                ResultSet res = statement.executeQuery();
                while (res.next()) {
                    list.add(new ChallengeSubmissionCommentDto(
                        res.getLong("csc.id"),
                        res.getString("csc.content"),
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
        ChallengeSubmissionCommentCreateDto challengeSubmissionComment,
        long userId
    ) {
        Supplier<Void> dbOperation = Unchecked.supplier(() -> {
            try (
                Connection c = dataSource.getConnection();
                PreparedStatement statement = c.prepareStatement(
                    "INSERT INTO challenge_submission_comment "
                        + "(content, user_id, challenge_submission_id) "
                        + "VALUES (?, ?, ?)"
                )
            ) {
                statement.setString(1, challengeSubmissionComment.content());
                statement.setLong(2, userId);
                statement.setLong(3, challengeSubmissionComment.submissionId());

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
