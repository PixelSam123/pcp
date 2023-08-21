package io.github.pixelsam123.pcp.challenge.submission;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ChallengeSubmissionRepository implements PanacheRepository<ChallengeSubmission> {
    public Uni<Long> asyncCountByChallengeIdAndUserId(Long challengeId, Long userId) {
        return Uni
            .createFrom()
            .item(() -> count("challengeId = ?1 and userId = ?2", challengeId, userId))
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    public Uni<Optional<ChallengeSubmission>> asyncFindById(Long id) {
        return Uni
            .createFrom()
            .item(() -> findByIdOptional(id))
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    public Uni<List<ChallengeSubmissionDto>> asyncListByChallengeId(Long challengeId) {
        return Uni
            .createFrom()
            .item(
                () -> find("challengeId", challengeId)
                    .project(ChallengeSubmissionDto.class)
                    .list()
            )
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    public Uni<Void> asyncPersist(ChallengeSubmission challengeSubmission) {
        return Uni
            .createFrom()
            .item(() -> {
                persist(challengeSubmission);

                return null;
            })
            .replaceWithVoid()
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }
}
