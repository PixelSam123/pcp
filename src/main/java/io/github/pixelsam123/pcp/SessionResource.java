package io.github.pixelsam123.pcp;

import io.github.pixelsam123.pcp.challenge.ChallengeBriefDto;
import io.github.pixelsam123.pcp.challenge.ChallengeCreateDto;
import io.github.pixelsam123.pcp.challenge.ChallengeRepository;
import io.github.pixelsam123.pcp.challenge.ChallengeSort;
import io.github.pixelsam123.pcp.challenge.submission.vote.ChallengeSubmissionVoteRepository;
import io.github.pixelsam123.pcp.challenge.vote.ChallengeVoteRepository;
import io.github.pixelsam123.pcp.common.HttpException;
import io.github.pixelsam123.pcp.user.UserBriefDto;
import io.github.pixelsam123.pcp.user.UserRepository;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Path("/session")
public class SessionResource {
    private final String cookieName;
    private final ChallengeRepository challengeRepository;
    private final ChallengeVoteRepository challengeVoteRepository;
    private final ChallengeSubmissionVoteRepository challengeSubmissionVoteRepository;
    private final UserRepository userRepository;

    public SessionResource(
        @ConfigProperty(name = "mp.jwt.token.cookie") String cookieName,
        ChallengeRepository challengeRepository,
        ChallengeVoteRepository challengeVoteRepository,
        ChallengeSubmissionVoteRepository challengeSubmissionVoteRepository,
        UserRepository userRepository
    ) {
        this.cookieName = cookieName;
        this.challengeRepository = challengeRepository;
        this.challengeVoteRepository = challengeVoteRepository;
        this.challengeSubmissionVoteRepository = challengeSubmissionVoteRepository;
        this.userRepository = userRepository;
    }

    @GET
    @RolesAllowed({"User"})
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<UserBriefDto> sessionUser(@Context SecurityContext ctx) {
        return userRepository
            .findBriefByName(ctx.getUserPrincipal().getName())
            .map(dbUser -> dbUser.orElseThrow(() -> new HttpException(
                Response.Status.NOT_FOUND,
                "Username of your session is not found"
            )));
    }

    @GET
    @Path("/challenges")
    @RolesAllowed({"User"})
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<List<ChallengeBriefDto>> sessionChallenges(@Context SecurityContext ctx) {
        return challengeRepository.list(
            List.of(1, 2, 3, 4, 5),
            ctx.getUserPrincipal().getName(),
            ChallengeSort.NEWEST
        );
    }

    @GET
    @Path("/challenges/name/{name}")
    @RolesAllowed({"User"})
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<ChallengeCreateDto> sessionChallenge(
        @PathParam("name") String name,
        @Context SecurityContext ctx
    ) {
        return challengeRepository
            .findCreateDtoByNameAndUserName(name, ctx.getUserPrincipal().getName())
            .map(dbChallenge -> dbChallenge.orElseThrow(() -> new HttpException(
                Response.Status.NOT_FOUND,
                "Challenge under your username Not Found"
            )));
    }

    @GET
    @Path("/challenge-votes/{challengeId}")
    @RolesAllowed({"User"})
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Optional<Boolean>> sessionChallengeIsUpvoteByChallengeId(
        @PathParam("challengeId") long challengeId, @Context SecurityContext ctx
    ) {
        return challengeVoteRepository.findIsUpvoteByChallengeIdAndUserName(
            challengeId,
            ctx.getUserPrincipal().getName()
        );
    }

    @GET
    @Path("/challenge-submission-votes/{challengeSubmissionId}")
    @RolesAllowed({"User"})
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Optional<Boolean>> sessionChallengeSubmissionIsUpvoteByChallengeSubmissionId(
        @PathParam("challengeSubmissionId") long challengeSubmissionId,
        @Context SecurityContext ctx
    ) {
        return challengeSubmissionVoteRepository.findIsUpvoteByChallengeSubmissionIdAndUserName(
            challengeSubmissionId,
            ctx.getUserPrincipal().getName()
        );
    }

    @POST
    @Path("/logout")
    @RolesAllowed({"User"})
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.WILDCARD)
    public Uni<Response> sessionLogout() {
        return Uni
            .createFrom()
            .item(() -> Response
                .ok()
                .cookie(new NewCookie.Builder(cookieName)
                    .value("")
                    .domain(null)
                    .path("/")
                    .expiry(Date.from(Instant.EPOCH))
                    .maxAge(0)
                    .httpOnly(true)
                    .sameSite(NewCookie.SameSite.STRICT)
                    .build())
                .build());
    }
}
