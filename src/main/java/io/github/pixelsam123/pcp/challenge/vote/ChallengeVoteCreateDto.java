package io.github.pixelsam123.pcp.challenge.vote;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public record ChallengeVoteCreateDto(
    @JsonProperty("isUpvote") @NotNull boolean isUpvote,
    @NotNull long challengeId
) {
}
