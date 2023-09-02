package io.github.pixelsam123.pcp.challenge;

import io.github.pixelsam123.pcp.user.User;

import java.sql.Timestamp;

public record Challenge(
    long id,
    Timestamp timestamp,
    String name,
    String description,
    String initialCode,
    String testCase,
    int tier,
    int completedCount,
    User user
) {
}
