package io.github.pixelsam123.pcp.challenge.submission;

import io.github.pixelsam123.pcp.challenge.Challenge;
import io.github.pixelsam123.pcp.user.User;

public record ChallengeSubmission(
    long id,
    String code,
    User user,
    Challenge challenge
) {
}
