package io.github.pixelsam123.pcp.challenge.submission.vote;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public record ChallengeSubmissionVoteCreateDto(
    @JsonProperty("isUpvote") @NotNull boolean isUpvote,
    @NotNull long submissionId
) {
}
