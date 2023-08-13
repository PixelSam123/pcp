package io.github.pixelsam123.pcp.challenge.submission.vote;

import io.github.pixelsam123.pcp.challenge.submission.ChallengeSubmission;
import io.github.pixelsam123.pcp.user.User;
import jakarta.persistence.*;

@Entity
public class ChallengeSubmissionVote {
    private long id;
    private boolean upvote;

    private User user;
    private ChallengeSubmission challengeSubmission;

    @Id
    @GeneratedValue
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Column(nullable = false)
    public boolean isUpvote() {
        return upvote;
    }

    public void setUpvote(boolean upvote) {
        this.upvote = upvote;
    }

    @ManyToOne
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @ManyToOne
    public ChallengeSubmission getChallengeSubmission() {
        return challengeSubmission;
    }

    public void setChallengeSubmission(ChallengeSubmission challengeSubmission) {
        this.challengeSubmission = challengeSubmission;
    }
}
