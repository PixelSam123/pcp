package io.github.pixelsam123.pcp.session;

import io.github.pixelsam123.pcp.user.UserBriefDto;

public record Session(
    UserBriefDto userInfo
) {
}
