package io.github.pixelsam123.pcp.challenge.submission.vote;

import io.github.pixelsam123.pcp.challenge.submission.ChallengeSubmission;
import io.github.pixelsam123.pcp.user.User;
import jakarta.persistence.*;

@Entity
public class ChallengeSubmissionVote {
    private Long id;
    private boolean upvote;

    private User user;
    private ChallengeSubmission challengeSubmission;

    @Id
    @GeneratedValue
    public Long getId() {
        return id;
    }

    @Column(nullable = false)
    public boolean isUpvote() {
        return upvote;
    }

    @ManyToOne
    public User getUser() {
        return user;
    }

    @ManyToOne
    public ChallengeSubmission getChallengeSubmission() {
        return challengeSubmission;
    }
}
