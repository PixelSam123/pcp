package io.github.pixelsam123.pcp.token;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Token {
    private final String accessToken;
    private final String tokenType;

    public Token(String accessToken, String tokenType) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
    }

    @JsonProperty("access_token")
    public String getAccessToken() {
        return accessToken;
    }

    @JsonProperty("token_type")
    public String getTokenType() {
        return tokenType;
    }
}
