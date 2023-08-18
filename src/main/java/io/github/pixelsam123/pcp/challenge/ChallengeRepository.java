package io.github.pixelsam123.pcp.challenge;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ChallengeRepository implements PanacheRepository<Challenge> {
}
