package io.github.pixelsam123.pcp.challenge.comment;

import io.github.pixelsam123.pcp.user.UserBriefDto;

public record ChallengeCommentDto(
    long id,
    String content,
    UserBriefDto user
) {
}
