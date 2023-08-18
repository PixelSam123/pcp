package io.github.pixelsam123.pcp.challenge.comment;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ChallengeCommentRepository implements PanacheRepository<ChallengeComment> {
}
