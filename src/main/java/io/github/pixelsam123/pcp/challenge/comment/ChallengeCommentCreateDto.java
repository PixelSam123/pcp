package io.github.pixelsam123.pcp.challenge.comment;

import jakarta.validation.constraints.NotNull;

public record ChallengeCommentCreateDto(
    @NotNull String content,
    @NotNull long challengeId
) {
}
