package io.github.pixelsam123.pcp.user;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class UserRepository implements PanacheRepository<User> {
    public Uni<Long> asyncCountByName(String name) {
        return Uni
            .createFrom()
            .item(() -> count("name", name))
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    public Uni<Optional<User>> asyncFindByName(String name) {
        return Uni
            .createFrom()
            .item(() -> find("name", name).firstResultOptional())
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    public Uni<Optional<UserBriefDto>> asyncFindByNameBrief(String name) {
        return Uni
            .createFrom()
            .item(() -> find("name", name).project(UserBriefDto.class).firstResultOptional())
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    public Uni<List<UserBriefDto>> asyncListAllBrief() {
        return Uni
            .createFrom()
            .item(() -> findAll().project(UserBriefDto.class).list())
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    public Uni<Void> asyncAddPoints(User user, int points) {
        return Uni
            .createFrom()
            .item(() -> {
                user.setPoints(user.getPoints() + points);

                return null;
            })
            .replaceWithVoid()
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    public Uni<Void> asyncPersist(User user) {
        return Uni
            .createFrom()
            .item(() -> {
                persist(user);

                return null;
            })
            .replaceWithVoid()
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }
}
