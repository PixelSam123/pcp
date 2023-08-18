package io.github.pixelsam123.pcp.challenge.submission.vote;

import io.github.pixelsam123.pcp.challenge.submission.ChallengeSubmission;
import io.github.pixelsam123.pcp.user.User;
import jakarta.persistence.*;

@Entity
public class ChallengeSubmissionVote {
    private Long id;
    private boolean isUpvote;

    private User user;
    private ChallengeSubmission challengeSubmission;

    @Id
    @GeneratedValue
    public Long getId() {
        return id;
    }

    @Column(nullable = false)
    public boolean getIsUpvote() {
        return isUpvote;
    }

    @ManyToOne
    public User getUser() {
        return user;
    }

    @ManyToOne
    public ChallengeSubmission getChallengeSubmission() {
        return challengeSubmission;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setIsUpvote(boolean isUpvote) {
        this.isUpvote = isUpvote;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setChallengeSubmission(ChallengeSubmission challengeSubmission) {
        this.challengeSubmission = challengeSubmission;
    }
}
