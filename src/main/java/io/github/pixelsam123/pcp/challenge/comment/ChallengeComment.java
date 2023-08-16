package io.github.pixelsam123.pcp.challenge.comment;

import io.github.pixelsam123.pcp.challenge.Challenge;
import io.github.pixelsam123.pcp.user.User;
import jakarta.persistence.*;

@Entity
public class ChallengeComment {
    private long id;
    private String content;

    private User user;
    private Challenge challenge;

    @Id
    @GeneratedValue
    public long getId() {
        return id;
    }

    @Column(nullable = false)
    public String getContent() {
        return content;
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

    public void setContent(String content) {
        this.content = content;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setChallenge(Challenge challenge) {
        this.challenge = challenge;
    }
}
