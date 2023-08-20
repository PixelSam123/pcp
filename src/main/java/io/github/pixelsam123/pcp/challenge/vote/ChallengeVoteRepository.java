package io.github.pixelsam123.pcp.challenge.vote;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ChallengeVoteRepository implements PanacheRepository<ChallengeVote> {
    public Uni<Long> asyncCountByChallengeIdAndUserId(Long challengeId, Long userId) {
        return Uni
            .createFrom()
            .item(() -> count("challengeId = ?1 and userId = ?2", challengeId, userId))
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    public Uni<Optional<ChallengeVote>> asyncFindById(Long id) {
        return Uni
            .createFrom()
            .item(() -> findByIdOptional(id))
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    public Uni<List<ChallengeVoteDto>> asyncListByChallengeId(Long challengeId) {
        return Uni
            .createFrom()
            .item(() -> find("challengeId", challengeId).project(ChallengeVoteDto.class).list())
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    public Uni<Void> asyncPersist(ChallengeVote challengeVote) {
        return Uni
            .createFrom()
            .item(() -> {
                persist(challengeVote);

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
