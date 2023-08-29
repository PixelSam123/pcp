package io.github.pixelsam123.pcp.challenge.comment;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class ChallengeCommentRepository {
    public Uni<List<ChallengeCommentDto>> asyncListByChallengeId(Long challengeId) {
        return Uni
            .createFrom()
            .item(() -> find("challengeId", challengeId).project(ChallengeCommentDto.class).list())
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    public Uni<Void> asyncPersist(ChallengeComment challengeComment) {
        return Uni
            .createFrom()
            .item(() -> {
                persist(challengeComment);

                return null;
            })
            .replaceWithVoid()
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }
}
