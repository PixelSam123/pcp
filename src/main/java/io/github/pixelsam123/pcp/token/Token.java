package io.github.pixelsam123.pcp.token;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Token(
    @JsonProperty("access_token") String accessToken,
    @JsonProperty("token_type") String tokenType
) {
}
