package io.github.pixelsam123.pcp.challenge.comment;

import io.github.pixelsam123.pcp.Utils;
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

@ApplicationScoped
public class ChallengeCommentRepository {
    private final DataSource dataSource;

    public ChallengeCommentRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Uni<List<ChallengeCommentDto>> listByChallengeId(long challengeId) {
        return Utils.runInWorkerPool(Unchecked.supplier(() -> {
            try (
                Connection c = dataSource.getConnection();
                PreparedStatement statement = c.prepareStatement(
                    "SELECT "
                        + "cc.id, "
                        + "cc.content, "
                        + "u.id, "
                        + "u.name, "
                        + "u.points "
                        + "FROM challenge_comment cc JOIN user u ON cc.user_id = u.id "
                        + "WHERE challenge_id = ?"
                )
            ) {
                statement.setLong(1, challengeId);

                List<ChallengeCommentDto> list = new ArrayList<>();

                ResultSet res = statement.executeQuery();
                while (res.next()) {
                    list.add(new ChallengeCommentDto(
                        res.getLong("cc.id"),
                        res.getString("cc.content"),
                        new UserBriefDto(
                            res.getLong("u.id"),
                            res.getString("u.name"),
                            res.getInt("u.points")
                        )
                    ));
                }

                return list;
            }
        }));
    }

    public Uni<Void> persist(ChallengeCommentCreateDto challengeComment, long userId) {
        return Utils.runInWorkerPool(Unchecked.supplier(() -> {
            try (
                Connection c = dataSource.getConnection();
                PreparedStatement statement = c.prepareStatement(
                    "INSERT INTO challenge_comment (content, user_id, challenge_id) "
                        + "VALUES (?, ?, ?)"
                )
            ) {
                statement.setString(1, challengeComment.content());
                statement.setLong(2, userId);
                statement.setLong(3, challengeComment.challengeId());

                if (statement.executeUpdate() < 1) {
                    throw new InternalServerErrorException(
                        "Insertion error: inserted row count is less than 1"
                    );
                }

                return null;
            }
        }));
    }
}
