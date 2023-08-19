package io.github.pixelsam123.pcp.challenge.submission.comment;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ChallengeSubmissionCommentRepository implements PanacheRepository<ChallengeSubmissionComment> {
}
