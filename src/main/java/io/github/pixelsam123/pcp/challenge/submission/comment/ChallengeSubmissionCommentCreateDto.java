package io.github.pixelsam123.pcp.challenge.submission.comment;

import jakarta.validation.constraints.NotNull;

public record ChallengeSubmissionCommentCreateDto(
    @NotNull String content,
    @NotNull long submissionId
) {
}
