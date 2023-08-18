package io.github.pixelsam123.pcp.challenge;

import io.github.pixelsam123.pcp.challenge.comment.ChallengeComment;
import io.github.pixelsam123.pcp.challenge.submission.ChallengeSubmission;
import io.github.pixelsam123.pcp.challenge.vote.ChallengeVote;
import io.github.pixelsam123.pcp.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Getter
@Setter
public class Challenge {
    @Id
    @GeneratedValue
    private Long id;
    @Column(unique = true)
    private String name;
    @Column(nullable = false)
    private String description;
    @Column(nullable = false)
    private String initialCode;
    @Column(nullable = false)
    private String testCase;
    @Column(nullable = false)
    private int tier;

    @ManyToOne
    private User user;

    @OneToMany(mappedBy = "challenge")
    private List<ChallengeSubmission> challengeSubmissions;
    @OneToMany(mappedBy = "challenge")
    private List<ChallengeComment> challengeComments;
    @OneToMany(mappedBy = "challenge")
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
}
