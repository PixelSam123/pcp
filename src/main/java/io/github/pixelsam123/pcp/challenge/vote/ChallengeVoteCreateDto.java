package io.github.pixelsam123.pcp.challenge.vote;

import jakarta.validation.constraints.NotNull;

public record ChallengeVoteCreateDto(
    @NotNull boolean isUpvote,
    @NotNull long challengeId
) {
}
