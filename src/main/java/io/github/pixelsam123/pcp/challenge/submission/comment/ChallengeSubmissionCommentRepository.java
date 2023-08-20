package io.github.pixelsam123.pcp.challenge.submission.comment;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class ChallengeSubmissionCommentRepository implements PanacheRepository<ChallengeSubmissionComment> {
    public Uni<List<ChallengeSubmissionCommentDto>> asyncListByChallengeSubmissionId(Long challengeSubmissionId) {
        return Uni
            .createFrom()
            .item(() -> find("challengeSubmissionId", challengeSubmissionId)
                .project(ChallengeSubmissionCommentDto.class)
                .list())
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    public Uni<Void> asyncPersist(ChallengeSubmissionComment challengeSubmissionComment) {
        return Uni
            .createFrom()
            .item(() -> {
                persist(challengeSubmissionComment);

                return null;
            })
            .replaceWithVoid()
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }
}
