package io.github.pixelsam123.pcp.challenge;

import io.github.pixelsam123.pcp.user.UserBriefDto;

public record ChallengeDto(
    long id,
    String name,
    int tier,
    int completedCount,
    UserBriefDto user,
    String description,
    String initialCode
) {
    public ChallengeDto(Challenge challenge) {
        this(challenge.getId(),
            challenge.getName(),
            challenge.getTier(),
            challenge.getCompletedCount(),
            new UserBriefDto(challenge.getUser()),
            challenge.getDescription(),
            challenge.getInitialCode()
        );
    }
}
