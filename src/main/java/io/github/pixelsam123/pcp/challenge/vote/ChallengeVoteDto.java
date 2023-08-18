package io.github.pixelsam123.pcp.challenge.vote;

import io.github.pixelsam123.pcp.user.UserBriefDto;

public record ChallengeVoteDto(
    long id,
    boolean isUpvote,
    UserBriefDto user
) {
    public ChallengeVoteDto(ChallengeVote challengeVote) {
        this(
            challengeVote.getId(),
            challengeVote.getIsUpvote(),
            new UserBriefDto(challengeVote.getUser())
        );
    }
}
