package io.github.pixelsam123.pcp.challenge.submission.vote;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.pixelsam123.pcp.user.UserBriefDto;

public record ChallengeSubmissionVoteDto(
    long id,
    @JsonProperty("isUpvote") boolean isUpvote,
    UserBriefDto user
) {
}
