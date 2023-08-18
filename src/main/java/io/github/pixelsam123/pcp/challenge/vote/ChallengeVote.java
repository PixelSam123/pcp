package io.github.pixelsam123.pcp.challenge.vote;

import io.github.pixelsam123.pcp.challenge.Challenge;
import io.github.pixelsam123.pcp.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class ChallengeVote {
    @Id
    @GeneratedValue
    private Long id;
    @Column(nullable = false)
    private boolean isUpvote;

    @ManyToOne
    private User user;
    @ManyToOne
    private Challenge challenge;

    public ChallengeVote() {
    }

    public ChallengeVote(
        ChallengeVoteCreateDto challengeVoteToCreate,
        User user,
        Challenge challenge
    ) {
        this.isUpvote = challengeVoteToCreate.isUpvote();
        this.user = user;
        this.challenge = challenge;
    }
}
