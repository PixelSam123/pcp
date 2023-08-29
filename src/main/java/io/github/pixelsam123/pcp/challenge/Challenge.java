package io.github.pixelsam123.pcp.challenge;

import io.github.pixelsam123.pcp.user.User;

public record Challenge(
    long id,
    String name,
    String description,
    String initialCode,
    String testCase,
    int tier,
    int completedCount,
    User user
) {
}
