package io.github.pixelsam123.pcp.user;

import io.github.pixelsam123.pcp.challenge.Challenge;
import io.github.pixelsam123.pcp.challenge.comment.ChallengeComment;
import io.github.pixelsam123.pcp.challenge.submission.ChallengeSubmission;
import io.github.pixelsam123.pcp.challenge.submission.comment.ChallengeSubmissionComment;
import io.github.pixelsam123.pcp.challenge.submission.vote.ChallengeSubmissionVote;
import io.github.pixelsam123.pcp.challenge.vote.ChallengeVote;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue
    private Long id;
    @Column(unique = true)
    private String name;
    @Column(nullable = false)
    private String passwordHash;
    @Column(columnDefinition = "int default 0")
    private int points;

    @OneToMany(mappedBy = "user")
    private List<Challenge> challenges;
    @OneToMany(mappedBy = "user")
    private List<ChallengeSubmission> challengeSubmissions;
    @OneToMany(mappedBy = "user")
    private List<ChallengeComment> challengeComments;
    @OneToMany(mappedBy = "user")
    private List<ChallengeSubmissionComment> challengeSubmissionComments;
    @OneToMany(mappedBy = "user")
    private List<ChallengeVote> challengeVotes;
    @OneToMany(mappedBy = "user")
    private List<ChallengeSubmissionVote> challengeSubmissionVotes;

    public User() {
    }

    public User(UserCreateDto userToCreate) {
        this.name = userToCreate.name();
        this.passwordHash = userToCreate.password();
        this.points = 0;
    }
}
