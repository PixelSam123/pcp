package io.github.pixelsam123.pcp.challenge;

import io.github.pixelsam123.pcp.challenge.comment.ChallengeComment;
import io.github.pixelsam123.pcp.challenge.submission.ChallengeSubmission;
import io.github.pixelsam123.pcp.challenge.vote.ChallengeVote;
import io.github.pixelsam123.pcp.user.User;
import jakarta.persistence.*;

import java.util.List;

@Entity
public class Challenge {
    private Long id;
    private String name;
    private String description;
    private String initialCode;
    private String testCase;
    private int tier;

    private User user;

    private List<ChallengeSubmission> challengeSubmissions;
    private List<ChallengeComment> challengeComments;
    private List<ChallengeVote> challengeVotes;

    public Challenge() {
    }

    public Challenge(ChallengeCreateDto challengeToCreate, User user) {
        this.name = challengeToCreate.name();
        this.description = challengeToCreate.description();
        this.initialCode = challengeToCreate.initialCode();
        this.testCase = challengeToCreate.testCase();
        this.tier = challengeToCreate.tier();
        this.user = user;
    }

    @Id
    @GeneratedValue
    public Long getId() {
        return id;
    }

    @Column(unique = true)
    public String getName() {
        return name;
    }

    @Column(nullable = false)
    public String getDescription() {
        return description;
    }

    @Column(nullable = false)
    public String getInitialCode() {
        return initialCode;
    }

    @Column(nullable = false)
    public String getTestCase() {
        return testCase;
    }

    @Column(nullable = false)
    public int getTier() {
        return tier;
    }

    @ManyToOne
    public User getUser() {
        return user;
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

    public void setId(long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setInitialCode(String initialCode) {
        this.initialCode = initialCode;
    }

    public void setTestCase(String testCase) {
        this.testCase = testCase;
    }

    public void setTier(int tier) {
        this.tier = tier;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setChallengeSubmissions(List<ChallengeSubmission> challengeSubmissions) {
        this.challengeSubmissions = challengeSubmissions;
    }

    public void setChallengeComments(List<ChallengeComment> challengeComments) {
        this.challengeComments = challengeComments;
    }

    public void setChallengeVotes(List<ChallengeVote> challengeVotes) {
        this.challengeVotes = challengeVotes;
    }
}
