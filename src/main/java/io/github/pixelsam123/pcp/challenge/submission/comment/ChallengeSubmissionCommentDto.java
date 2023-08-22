package io.github.pixelsam123.pcp.challenge.submission.comment;

import io.github.pixelsam123.pcp.user.UserBriefDto;

public record ChallengeSubmissionCommentDto(
    long id,
    String content,
    UserBriefDto user
) {
    public ChallengeSubmissionCommentDto(ChallengeSubmissionComment challengeSubmissionComment) {
        this(
            challengeSubmissionComment.getId(),
            challengeSubmissionComment.getContent(),
            new UserBriefDto(challengeSubmissionComment.getUser())
        );
    }
}
