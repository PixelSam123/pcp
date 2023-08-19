package io.github.pixelsam123.pcp.challenge.submission;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ChallengeSubmissionRepository implements PanacheRepository<ChallengeSubmission> {
}
