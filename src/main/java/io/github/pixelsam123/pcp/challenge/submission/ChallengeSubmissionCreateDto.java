package io.github.pixelsam123.pcp.challenge.submission;

import jakarta.validation.constraints.NotNull;

public record ChallengeSubmissionCreateDto(
    @NotNull String code,
    @NotNull long challengeId
) {
}
