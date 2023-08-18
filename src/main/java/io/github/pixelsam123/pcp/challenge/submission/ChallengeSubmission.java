package io.github.pixelsam123.pcp.challenge.submission;

import io.github.pixelsam123.pcp.challenge.Challenge;
import io.github.pixelsam123.pcp.challenge.submission.comment.ChallengeSubmissionComment;
import io.github.pixelsam123.pcp.challenge.submission.vote.ChallengeSubmissionVote;
import io.github.pixelsam123.pcp.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Getter
@Setter
public class ChallengeSubmission {
    @Id
    @GeneratedValue
    private Long id;
    @Column(nullable = false)
    private String code;

    @ManyToOne
    private User user;
    @ManyToOne
    private Challenge challenge;

    @OneToMany(mappedBy = "challengeSubmission")
    private List<ChallengeSubmissionComment> challengeSubmissionComments;
    @OneToMany(mappedBy = "challengeSubmission")
    private List<ChallengeSubmissionVote> challengeSubmissionVotes;
}
