package io.github.pixelsam123.pcp.challenge;

import io.github.pixelsam123.pcp.challenge.comment.ChallengeComment;
import io.github.pixelsam123.pcp.challenge.submission.ChallengeSubmission;
import io.github.pixelsam123.pcp.challenge.vote.ChallengeVote;
import io.github.pixelsam123.pcp.user.User;
import jakarta.persistence.*;

import java.util.List;

@Entity
public class Challenge {
    private long id;
    private String name;
    private String description;
    private String initialCode;
    private String testCase;
    private int tier;

    private User user;

    private List<ChallengeSubmission> challengeSubmissions;
    private List<ChallengeComment> challengeComments;
    private List<ChallengeVote> challengeVotes;

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
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Column(nullable = false)
    public String getInitialCode() {
        return initialCode;
    }

    public void setInitialCode(String initialCode) {
        this.initialCode = initialCode;
    }

    @Column(nullable = false)
    public String getTestCase() {
        return testCase;
    }

    public void setTestCase(String testCase) {
        this.testCase = testCase;
    }

    @Column(nullable = false)
    public int getTier() {
        return tier;
    }

    public void setTier(int tier) {
        this.tier = tier;
    }

    @ManyToOne
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @OneToMany(mappedBy = "challenge")
    public List<ChallengeSubmission> getChallengeSubmissions() {
        return challengeSubmissions;
    }

    @OneToMany(mappedBy = "challenge")
    public List<ChallengeComment> getChallengeComments() {
        return challengeComments;
    }

    @OneToMany(mappedBy = "challenge")
    public List<ChallengeVote> getChallengeVotes() {
        return challengeVotes;
    }
}
