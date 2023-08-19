package io.github.pixelsam123.pcp.challenge.submission.vote;

import jakarta.validation.constraints.NotNull;

public record ChallengeSubmissionVoteCreateDto(
    @NotNull boolean isUpvote,
    @NotNull long submissionId
) {
}
