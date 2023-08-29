package io.github.pixelsam123.pcp.challenge.submission.comment;

import io.github.pixelsam123.pcp.user.UserBriefDto;

public record ChallengeSubmissionCommentDto(
    long id,
    String content,
    UserBriefDto user
) {
}
