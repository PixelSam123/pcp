package io.github.pixelsam123.pcp.challenge;

public record ChallengeSecuredDto(
    long id,
    String name,
    int tier,
    String description,
    String initialCode,
    String testCase
) {
}
