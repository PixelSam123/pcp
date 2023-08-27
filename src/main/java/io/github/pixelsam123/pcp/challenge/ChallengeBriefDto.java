package io.github.pixelsam123.pcp.challenge;

import io.github.pixelsam123.pcp.user.UserBriefDto;

public record ChallengeBriefDto(
    long id,
    String name,
    int tier,
    int completedCount,
    UserBriefDto user
) {
}
