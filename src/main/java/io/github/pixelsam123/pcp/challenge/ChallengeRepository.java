package io.github.pixelsam123.pcp.challenge;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ChallengeRepository implements PanacheRepository<Challenge> {
    public Uni<Long> asyncCountByName(String name) {
        return Uni
            .createFrom()
            .item(() -> count("name", name))
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    public Uni<Optional<Challenge>> asyncFindById(Long id) {
        return Uni
            .createFrom()
            .item(() -> findByIdOptional(id))
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    public Uni<Optional<Challenge>> asyncFindByName(String name) {
        return Uni
            .createFrom()
            .item(() -> find("name", name).firstResultOptional())
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    public Uni<Optional<ChallengeDto>> asyncFindByNameDto(String name) {
        return Uni
            .createFrom()
            .item(() -> find("name", name).project(ChallengeDto.class).firstResultOptional())
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    public Uni<List<ChallengeBriefDto>> asyncListAllBrief() {
        return Uni
            .createFrom()
            .item(() -> findAll().project(ChallengeBriefDto.class).list())
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    public Uni<Void> asyncPersist(Challenge challenge) {
        return Uni
            .createFrom()
            .item(() -> {
                persist(challenge);

                return null;
            })
            .replaceWithVoid()
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }
}
