package io.github.pixelsam123.pcp.challenge.submission;

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

@ApplicationScoped
public class ChallengeSubmissionRepository {
    private final DataSource dataSource;

    public ChallengeSubmissionRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Uni<Long> countByChallengeIdAndUserId(long challengeId, long userId) {
        return Utils.runInWorkerPool(() -> {
            try (
                Connection c = dataSource.getConnection();
                PreparedStatement statement = c.prepareStatement(
                    "SELECT COUNT(*) FROM challenge_submission "
                        + "WHERE challenge_id = ? AND user_id = ?"
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

    public Uni<Long> countById(long id) {
        return Utils.runInWorkerPool(() -> {
            try (
                Connection c = dataSource.getConnection();
                PreparedStatement statement = c.prepareStatement(
                    "SELECT COUNT(*) FROM challenge_submission WHERE id = ?"
                )
            ) {
                statement.setLong(1, id);

                ResultSet res = statement.executeQuery();
                res.next();

                return res.getLong(1);
            }
        });
    }

    public Uni<List<ChallengeSubmissionDto>> listByChallengeId(long challengeId) {
        return Utils.runInWorkerPool(() -> {
            try (
                Connection c = dataSource.getConnection();
                PreparedStatement statement = c.prepareStatement(
                    "SELECT "
                        + "cs.id, "
                        + "cs.code, "
                        + "u.id,"
                        + "u.name,"
                        + "u.points "
                        + "FROM challenge_submission cs JOIN user u ON cs.user_id = u.id "
                        + "WHERE cs.challenge_id = ? ORDER BY cs.id DESC"
                )
            ) {
                statement.setLong(1, challengeId);

                List<ChallengeSubmissionDto> list = new ArrayList<>();

                ResultSet res = statement.executeQuery();
                while (res.next()) {
                    list.add(new ChallengeSubmissionDto(
                        res.getLong("cs.id"),
                        res.getString("cs.code"),
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

    public Uni<Void> persist(ChallengeSubmissionCreateDto challengeSubmission, long userId) {
        return Utils.runInWorkerPool(() -> {
            try (
                Connection c = dataSource.getConnection();
                PreparedStatement statement = c.prepareStatement(
                    "INSERT INTO challenge_submission (code, user_id, challenge_id) "
                        + "VALUES (?, ?, ?)"
                )
            ) {
                statement.setString(1, challengeSubmission.code());
                statement.setLong(2, userId);
                statement.setLong(3, challengeSubmission.challengeId());

                if (statement.executeUpdate() < 1) {
                    throw new RuntimeException("Insert error: inserted row count is less than 1");
                }

                return null;
            }
        });
    }
}
