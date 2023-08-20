package io.github.pixelsam123.pcp.challenge.submission.vote;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ChallengeSubmissionVoteRepository implements PanacheRepository<ChallengeSubmissionVote> {
    public Uni<Long> asyncCountByChallengeSubmissionIdAndUserId(
        Long challengeSubmissionId,
        Long userId
    ) {
        return Uni
            .createFrom()
            .item(() -> count(
                "challengeSubmissionId = ?1 and userId = ?2",
                challengeSubmissionId,
                userId
            ))
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    public Uni<Optional<ChallengeSubmissionVote>> asyncFindById(Long id) {
        return Uni
            .createFrom()
            .item(() -> findByIdOptional(id))
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    public Uni<List<ChallengeSubmissionVoteDto>> asyncListByChallengeSubmissionId(
        Long challengeSubmissionId
    ) {
        return Uni
            .createFrom()
            .item(() -> find("challengeSubmissionId", challengeSubmissionId).project(
                ChallengeSubmissionVoteDto.class).list()
            )
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    public Uni<Void> asyncPersist(ChallengeSubmissionVote challengeSubmissionVote) {
        return Uni
            .createFrom()
            .item(() -> {
                persist(challengeSubmissionVote);

                return null;
            })
            .replaceWithVoid()
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    public Uni<Boolean> asyncDeleteById(Long id) {
        return Uni
            .createFrom()
            .item(() -> deleteById(id))
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }
}
