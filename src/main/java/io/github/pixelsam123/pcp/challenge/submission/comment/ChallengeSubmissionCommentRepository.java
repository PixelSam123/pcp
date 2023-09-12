package io.github.pixelsam123.pcp.challenge.submission.comment;

import io.github.pixelsam123.pcp.Utils;
import io.github.pixelsam123.pcp.user.UserBriefDto;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class ChallengeSubmissionCommentRepository {
    private final DataSource dataSource;

    public ChallengeSubmissionCommentRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Uni<List<ChallengeSubmissionCommentDto>> listByChallengeSubmissionId(long challengeSubmissionId) {
        return Utils.runInWorkerPool(() -> {
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
    }

    public Uni<Void> persist(
        ChallengeSubmissionCommentCreateDto challengeSubmissionComment,
        long userId
    ) {
        return Utils.runInWorkerPool(() -> {
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
                statement.setLong(3, challengeSubmissionComment.challengeSubmissionId());

                if (statement.executeUpdate() < 1) {
                    throw new RuntimeException("Insert error: inserted row count is less than 1");
                }

                return null;
            }
        });
    }
}
