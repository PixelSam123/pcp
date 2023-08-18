package io.github.pixelsam123.pcp.challenge.submission.comment;

import io.github.pixelsam123.pcp.challenge.submission.ChallengeSubmission;
import io.github.pixelsam123.pcp.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class ChallengeSubmissionComment {
    @Id
    @GeneratedValue
    private Long id;
    @Column(nullable = false)
    private String content;

    @ManyToOne
    private User user;
    @ManyToOne
    private ChallengeSubmission challengeSubmission;
}
