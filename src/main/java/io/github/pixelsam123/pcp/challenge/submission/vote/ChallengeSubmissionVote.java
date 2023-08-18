package io.github.pixelsam123.pcp.challenge.submission.vote;

import io.github.pixelsam123.pcp.challenge.submission.ChallengeSubmission;
import io.github.pixelsam123.pcp.user.User;
import jakarta.persistence.*;

@Entity
public class ChallengeSubmissionVote {
    @Id
    @GeneratedValue
    private Long id;
    @Column(nullable = false)
    private boolean isUpvote;

    @ManyToOne
    private User user;
    @ManyToOne
    private ChallengeSubmission challengeSubmission;
}
