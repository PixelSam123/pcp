package io.github.pixelsam123.pcp.challenge.submission.vote;

import io.github.pixelsam123.pcp.challenge.submission.ChallengeSubmission;
import io.github.pixelsam123.pcp.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class ChallengeSubmissionVote {
    @Id
    @GeneratedValue
    private Long id;
    @Column(nullable = false)
    private boolean isUpvote;

    @ManyToOne
    private User user;
    @ManyToOne
    private ChallengeSubmission challengeSubmission;

    public ChallengeSubmissionVote() {
    }

    public ChallengeSubmissionVote(
        ChallengeSubmissionVoteCreateDto challengeSubmissionVoteToCreate,
        User user,
        ChallengeSubmission challengeSubmission
    ) {
        this.isUpvote = challengeSubmissionVoteToCreate.isUpvote();
        this.user = user;
        this.challengeSubmission = challengeSubmission;
    }
}
