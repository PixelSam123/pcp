package io.github.pixelsam123.pcp.challenge.vote;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ChallengeVoteRepository implements PanacheRepository<ChallengeVote> {
}
