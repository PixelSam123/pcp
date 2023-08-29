package io.github.pixelsam123.pcp.challenge.vote;

import io.github.pixelsam123.pcp.challenge.Challenge;
import io.github.pixelsam123.pcp.user.User;

public record ChallengeVote(
    long id,
    boolean isUpvote,
    User user,
    Challenge challenge
) {
}
