package io.github.pixelsam123.pcp.challenge.submission.vote;

import io.github.pixelsam123.pcp.user.UserBriefDto;

public record ChallengeSubmissionVoteDto(
    long id,
    boolean isUpvote,
    UserBriefDto user
) {
}
