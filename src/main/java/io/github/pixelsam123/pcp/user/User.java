package io.github.pixelsam123.pcp.user;

public record User(
    long id,
    String name,
    String passwordHash,
    int points
) {
}
