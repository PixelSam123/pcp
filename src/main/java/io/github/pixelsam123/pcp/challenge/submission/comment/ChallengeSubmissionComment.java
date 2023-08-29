package io.github.pixelsam123.pcp.challenge.submission.comment;

import io.github.pixelsam123.pcp.challenge.submission.ChallengeSubmission;
import io.github.pixelsam123.pcp.user.User;

public record ChallengeSubmissionComment(
    long id,
    String content,
    User user,
    ChallengeSubmission challengeSubmission
) {
}
