package io.github.pixelsam123.pcp.challenge.submission;

import io.github.pixelsam123.pcp.challenge.Challenge;
import io.github.pixelsam123.pcp.challenge.submission.comment.ChallengeSubmissionComment;
import io.github.pixelsam123.pcp.challenge.submission.vote.ChallengeSubmissionVote;
import io.github.pixelsam123.pcp.user.User;
import jakarta.persistence.*;

import java.util.List;

@Entity
public class ChallengeSubmission {
    private long id;
    private String code;

    private User user;
    private Challenge challenge;

    private List<ChallengeSubmissionComment> challengeSubmissionComments;
    private List<ChallengeSubmissionVote> challengeSubmissionVotes;

    @Id
    @GeneratedValue
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Column(nullable = false)
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
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

    @OneToMany(mappedBy = "challengeSubmission")
    public List<ChallengeSubmissionComment> getChallengeSubmissionComments() {
        return challengeSubmissionComments;
    }

    @OneToMany(mappedBy = "challengeSubmission")
    public List<ChallengeSubmissionVote> getChallengeSubmissionVotes() {
        return challengeSubmissionVotes;
    }
}
