package io.github.pixelsam123.pcp.challenge.vote;

import io.github.pixelsam123.pcp.challenge.Challenge;
import io.github.pixelsam123.pcp.user.User;
import jakarta.persistence.*;

@Entity
public class ChallengeVote {
    private Long id;
    private boolean isUpvote;

    private User user;
    private Challenge challenge;

    public ChallengeVote() {
    }

    public ChallengeVote(
        ChallengeVoteCreateDto challengeVoteToCreate,
        User user,
        Challenge challenge
    ) {
        this.isUpvote = challengeVoteToCreate.isUpvote();
        this.user = user;
        this.challenge = challenge;
    }

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
    public Challenge getChallenge() {
        return challenge;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setIsUpvote(boolean isUpvote) {
        this.isUpvote = isUpvote;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setChallenge(Challenge challenge) {
        this.challenge = challenge;
    }
}
