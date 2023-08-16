package io.github.pixelsam123.pcp.challenge.submission.comment;

import io.github.pixelsam123.pcp.challenge.submission.ChallengeSubmission;
import io.github.pixelsam123.pcp.user.User;
import jakarta.persistence.*;

@Entity
public class ChallengeSubmissionComment {
    private long id;
    private String content;

    private User user;
    private ChallengeSubmission challengeSubmission;

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
    public ChallengeSubmission getChallengeSubmission() {
        return challengeSubmission;
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

    public void setChallengeSubmission(ChallengeSubmission challengeSubmission) {
        this.challengeSubmission = challengeSubmission;
    }
}
