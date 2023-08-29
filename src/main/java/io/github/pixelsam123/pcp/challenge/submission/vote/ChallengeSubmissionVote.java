package io.github.pixelsam123.pcp.challenge.submission.vote;

import io.github.pixelsam123.pcp.challenge.submission.ChallengeSubmission;
import io.github.pixelsam123.pcp.user.User;

public record ChallengeSubmissionVote(
    long id,
    boolean isUpvote,
    User user,
    ChallengeSubmission challengeSubmission
) {
}
