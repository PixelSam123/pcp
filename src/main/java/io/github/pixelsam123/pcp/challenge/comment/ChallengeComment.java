package io.github.pixelsam123.pcp.challenge.comment;

import io.github.pixelsam123.pcp.challenge.Challenge;
import io.github.pixelsam123.pcp.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class ChallengeComment {
    @Id
    @GeneratedValue
    private Long id;
    @Column(nullable = false)
    private String content;

    @ManyToOne
    private User user;
    @ManyToOne
    private Challenge challenge;

    public ChallengeComment() {
    }

    public ChallengeComment(
        ChallengeCommentCreateDto challengeCommentToCreate,
        User user,
        Challenge challenge
    ) {
        this.content = challengeCommentToCreate.content();
        this.user = user;
        this.challenge = challenge;
    }
}
