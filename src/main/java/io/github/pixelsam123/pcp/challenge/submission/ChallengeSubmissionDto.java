package io.github.pixelsam123.pcp.challenge.submission;

import io.github.pixelsam123.pcp.user.UserBriefDto;

public record ChallengeSubmissionDto(
    long id,
    String code,
    UserBriefDto user
) {
    public ChallengeSubmissionDto(ChallengeSubmission challengeSubmission) {
        this(
            challengeSubmission.getId(),
            challengeSubmission.getCode(),
            new UserBriefDto(challengeSubmission.getUser())
        );
    }
}
