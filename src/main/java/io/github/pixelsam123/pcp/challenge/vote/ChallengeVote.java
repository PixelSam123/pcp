package io.github.pixelsam123.pcp.challenge.vote;

import io.github.pixelsam123.pcp.challenge.Challenge;
import io.github.pixelsam123.pcp.user.User;
import jakarta.persistence.*;

@Entity
public class ChallengeVote {
    private long id;
    private boolean upvote;

    private User user;
    private Challenge challenge;

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
    public Challenge getChallenge() {
        return challenge;
    }

    public void setChallenge(Challenge challenge) {
        this.challenge = challenge;
    }
}
