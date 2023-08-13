package io.github.pixelsam123.pcp.user;

import io.github.pixelsam123.pcp.challenge.Challenge;
import io.github.pixelsam123.pcp.challenge.comment.ChallengeComment;
import io.github.pixelsam123.pcp.challenge.submission.ChallengeSubmission;
import io.github.pixelsam123.pcp.challenge.submission.comment.ChallengeSubmissionComment;
import io.github.pixelsam123.pcp.challenge.submission.vote.ChallengeSubmissionVote;
import io.github.pixelsam123.pcp.challenge.vote.ChallengeVote;
import jakarta.persistence.*;

import java.util.List;

@Entity
public class User {
    private long id;
    private String name;
    private String passwordHash;
    private int points;

    private List<Challenge> challenges;
    private List<ChallengeSubmission> challengeSubmissions;
    private List<ChallengeComment> challengeComments;
    private List<ChallengeSubmissionComment> challengeSubmissionComments;
    private List<ChallengeVote> challengeVotes;
    private List<ChallengeSubmissionVote> challengeSubmissionVotes;

    @Id
    @GeneratedValue
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Column(unique = true)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column(nullable = false)
    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    @Column(columnDefinition = "int default 0")
    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    @OneToMany(mappedBy = "user")
    public List<Challenge> getChallenges() {
        return challenges;
    }

    @OneToMany(mappedBy = "user")
    public List<ChallengeSubmission> getChallengeSubmissions() {
        return challengeSubmissions;
    }

    @OneToMany(mappedBy = "user")
    public List<ChallengeComment> getChallengeComments() {
        return challengeComments;
    }

    @OneToMany(mappedBy = "user")
    public List<ChallengeSubmissionComment> getChallengeSubmissionComments() {
        return challengeSubmissionComments;
    }

    @OneToMany(mappedBy = "user")
    public List<ChallengeVote> getChallengeVotes() {
        return challengeVotes;
    }

    @OneToMany(mappedBy = "user")
    public List<ChallengeSubmissionVote> getChallengeSubmissionVotes() {
        return challengeSubmissionVotes;
    }
}
