package io.github.pixelsam123.pcp.user;

public record UserCredentialsView(
    long userId,
    String passwordHash
) {
}
