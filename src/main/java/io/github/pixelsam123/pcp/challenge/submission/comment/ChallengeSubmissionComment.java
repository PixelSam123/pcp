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

    public void setId(long id) {
        this.id = id;
    }

    @Column(nullable = false)
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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
