package io.github.pixelsam123.pcp.challenge.submission;

import io.github.pixelsam123.pcp.challenge.Challenge;
import io.github.pixelsam123.pcp.challenge.submission.comment.ChallengeSubmissionComment;
import io.github.pixelsam123.pcp.challenge.submission.vote.ChallengeSubmissionVote;
import io.github.pixelsam123.pcp.user.User;
import jakarta.persistence.*;

import java.util.List;

@Entity
public class ChallengeSubmission {
    private Long id;
    private String code;

    private User user;
    private Challenge challenge;

    private List<ChallengeSubmissionComment> challengeSubmissionComments;
    private List<ChallengeSubmissionVote> challengeSubmissionVotes;

    @Id
    @GeneratedValue
    public Long getId() {
        return id;
    }

    @Column(nullable = false)
    public String getCode() {
        return code;
    }

    @ManyToOne
    public User getUser() {
        return user;
    }

    @ManyToOne
    public Challenge getChallenge() {
        return challenge;
    }

    @OneToMany(mappedBy = "challengeSubmission")
    public List<ChallengeSubmissionComment> getChallengeSubmissionComments() {
        return challengeSubmissionComments;
    }

    @OneToMany(mappedBy = "challengeSubmission")
    public List<ChallengeSubmissionVote> getChallengeSubmissionVotes() {
        return challengeSubmissionVotes;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setChallenge(Challenge challenge) {
        this.challenge = challenge;
    }

    public void setChallengeSubmissionComments(List<ChallengeSubmissionComment> challengeSubmissionComments) {
        this.challengeSubmissionComments = challengeSubmissionComments;
    }

    public void setChallengeSubmissionVotes(List<ChallengeSubmissionVote> challengeSubmissionVotes) {
        this.challengeSubmissionVotes = challengeSubmissionVotes;
    }
}
