package io.github.pixelsam123.pcp.challenge.comment;

import io.github.pixelsam123.pcp.challenge.Challenge;
import io.github.pixelsam123.pcp.user.User;

public record ChallengeComment(
    long id,
    String content,
    User user,
    Challenge challenge
) {
}
