package io.github.pixelsam123.pcp.user;

public record UserBriefDto(
    long id,
    String name,
    int points
) {
    public UserBriefDto(User user) {
        this(user.getId(), user.getName(), user.getPoints());
    }
}
