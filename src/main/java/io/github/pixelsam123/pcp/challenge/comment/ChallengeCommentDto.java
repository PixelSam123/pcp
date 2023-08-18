package io.github.pixelsam123.pcp.challenge.comment;

import io.github.pixelsam123.pcp.user.UserBriefDto;

public record ChallengeCommentDto(
    long id,
    String content,
    UserBriefDto userBriefDto
) {
    public ChallengeCommentDto(ChallengeComment challengeComment) {
        this(
            challengeComment.getId(),
            challengeComment.getContent(),
            new UserBriefDto(challengeComment.getUser())
        );
    }
}
