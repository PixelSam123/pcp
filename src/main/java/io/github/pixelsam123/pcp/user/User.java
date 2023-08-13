package io.github.pixelsam123.pcp.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

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
}
