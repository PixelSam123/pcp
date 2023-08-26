package io.github.pixelsam123.pcp.user;

import io.github.pixelsam123.pcp.challenge.Challenge;
import io.github.pixelsam123.pcp.challenge.comment.ChallengeComment;
import io.github.pixelsam123.pcp.challenge.submission.ChallengeSubmission;
import io.github.pixelsam123.pcp.challenge.submission.comment.ChallengeSubmissionComment;
import io.github.pixelsam123.pcp.challenge.submission.vote.ChallengeSubmissionVote;
import io.github.pixelsam123.pcp.challenge.vote.ChallengeVote;
import io.quarkus.security.jpa.Password;
import io.quarkus.security.jpa.Roles;
import io.quarkus.security.jpa.Username;
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
    @Username
    private String name;
    @Column(nullable = false)
    @Password
    private String passwordHash;
    @Roles
    private String role;
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

    public User(UserCreateDto userToCreate, String passwordHash) {
        this.name = userToCreate.name();
        this.passwordHash = passwordHash;
        this.role = "User";
        this.points = 0;
    }
}
