package io.github.pixelsam123.pcp.challenge.submission.vote;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ChallengeSubmissionVoteRepository implements PanacheRepository<ChallengeSubmissionVote> {
}
