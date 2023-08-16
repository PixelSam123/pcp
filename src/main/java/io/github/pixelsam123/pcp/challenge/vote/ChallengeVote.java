package io.github.pixelsam123.pcp.challenge.vote;

import io.github.pixelsam123.pcp.challenge.Challenge;
import io.github.pixelsam123.pcp.user.User;
import jakarta.persistence.*;

@Entity
public class ChallengeVote {
    private Long id;
    private boolean upvote;

    private User user;
    private Challenge challenge;

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
    public Challenge getChallenge() {
        return challenge;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setUpvote(boolean upvote) {
        this.upvote = upvote;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setChallenge(Challenge challenge) {
        this.challenge = challenge;
    }
}
