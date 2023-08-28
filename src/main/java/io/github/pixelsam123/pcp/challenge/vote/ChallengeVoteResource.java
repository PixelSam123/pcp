package io.github.pixelsam123.pcp.challenge.vote;

import io.github.pixelsam123.pcp.challenge.Challenge;
import io.github.pixelsam123.pcp.challenge.ChallengeRepository;
import io.github.pixelsam123.pcp.user.User;
import io.github.pixelsam123.pcp.user.UserRepository;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import jakarta.annotation.security.RolesAllowed;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.ResponseStatus;
import org.jboss.resteasy.reactive.RestResponse;

import java.util.List;
import java.util.Optional;

@Tag(name = "challenge_votes", description = "Challenge vote creation, viewing and editing")
@Path("/challenge_votes")
public class ChallengeVoteResource {
    private final ChallengeRepository challengeRepository;
    private final ChallengeVoteRepository challengeVoteRepository;
    private final UserRepository userRepository;

    public ChallengeVoteResource(
        ChallengeRepository challengeRepository,
        ChallengeVoteRepository challengeVoteRepository,
        UserRepository userRepository
    ) {
        this.challengeRepository = challengeRepository;
        this.challengeVoteRepository = challengeVoteRepository;
        this.userRepository = userRepository;
    }

    @POST
    @RolesAllowed({"User"})
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Uni<ChallengeVoteDto> createChallengeVote(
        ChallengeVoteCreateDto challengeVoteToCreate, @Context SecurityContext ctx
    ) {
        Uni<User> existingUserRetrieval = userRepository
            .findByName(ctx.getUserPrincipal().getName())
            .map(Unchecked.function(dbUser -> {
                if (dbUser.isEmpty()) {
                    throw new BadRequestException(
                        Response
                            .status(Response.Status.BAD_REQUEST)
                            .entity("User of your credentials doesn't exist")
                            .build()
                    );
                }

                return dbUser.get();
            }));

        Uni<Optional<Challenge>> challengeRetrieval =
            challengeRepository.asyncFindById(challengeVoteToCreate.challengeId());

        Uni<Long> challengeVoteCountRetrieval = existingUserRetrieval.flatMap(
            existingDbUser -> challengeVoteRepository.asyncCountByChallengeIdAndUserId(
                challengeVoteToCreate.challengeId(), existingDbUser.getId()
            )
        );

        return Uni
            .combine()
            .all()
            .unis(existingUserRetrieval, challengeRetrieval, challengeVoteCountRetrieval)
            .asTuple()
            .map(Unchecked.function((tuple) -> {
                User existingDbUser = tuple.getItem1();
                Optional<Challenge> dbChallenge = tuple.getItem2();
                long dbChallengeVoteCount = tuple.getItem3();

                if (dbChallenge.isEmpty()) {
                    throw new BadRequestException(
                        Response
                            .status(Response.Status.BAD_REQUEST)
                            .entity("Challenge doesn't exist")
                            .build()
                    );
                }

                if (dbChallengeVoteCount > 0) {
                    throw new BadRequestException(
                        Response
                            .status(Response.Status.BAD_REQUEST)
                            .entity("User already voted on this challenge")
                            .build()
                    );
                }

                return new ChallengeVote(challengeVoteToCreate, existingDbUser, dbChallenge.get());
            }))
            .flatMap(
                challengeVote -> challengeVoteRepository
                    .asyncPersist(challengeVote)
                    .map((unused) -> new ChallengeVoteDto(challengeVote))
            );
    }

    @GET
    @Path("/{challenge_name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<List<ChallengeVoteDto>> getChallengeVotesByChallengeName(
        @PathParam("challenge_name") String challengeName
    ) {
        Uni<Long> challengeIdRetrieval = challengeRepository
            .asyncFindByName(challengeName)
            .map(dbChallenge -> dbChallenge.map(Challenge::getId))
            .map(Unchecked.function(dbChallengeId -> {
                if (dbChallengeId.isEmpty()) {
                    throw new NotFoundException(
                        Response
                            .status(Response.Status.NOT_FOUND)
                            .entity("Challenge Not Found")
                            .build()
                    );
                }

                return dbChallengeId.get();
            }));

        return challengeIdRetrieval.flatMap(challengeVoteRepository::asyncListByChallengeId);
    }

    @DELETE
    @RolesAllowed({"User"})
    @Path("/{id}")
    @ResponseStatus(RestResponse.StatusCode.NO_CONTENT)
    public Uni<Void> deleteChallengeVote(@PathParam("id") long id, @Context SecurityContext ctx) {
        Uni<Optional<Long>> userIdRetrieval = userRepository
            .findByName(ctx.getUserPrincipal().getName())
            .map(dbUser -> dbUser.map(User::getId));

        Uni<Optional<ChallengeVote>> challengeVoteRetrieval =
            challengeVoteRepository.asyncFindById(id);

        return Uni
            .combine()
            .all()
            .unis(userIdRetrieval, challengeVoteRetrieval)
            .combinedWith(Unchecked.function((dbUserId, dbChallengeVote) -> {
                if (dbUserId.isEmpty()) {
                    throw new BadRequestException(
                        Response
                            .status(Response.Status.BAD_REQUEST)
                            .entity("User of your credentials doesn't exist")
                            .build()
                    );
                }

                if (dbChallengeVote.isEmpty()) {
                    throw new NotFoundException(
                        Response
                            .status(Response.Status.NOT_FOUND)
                            .entity("Challenge Vote Not Found")
                            .build()
                    );
                }

                Long existingDbUserId = dbUserId.get();
                ChallengeVote existingDbChallengeVote = dbChallengeVote.get();

                if (!existingDbUserId.equals(existingDbChallengeVote.getUser().getId())) {
                    throw new ForbiddenException(
                        Response
                            .status(Response.Status.FORBIDDEN)
                            .entity("Not allowed to delete on another user's behalf")
                            .build()
                    );
                }

                return null;
            }))
            .flatMap(unused -> challengeVoteRepository
                .asyncDeleteById(id)
                .map(Unchecked.function((Boolean isDeleted) -> {
                    if (!isDeleted) {
                        throw new InternalServerErrorException(
                            Response
                                .status(Response.Status.INTERNAL_SERVER_ERROR)
                                .entity("Failed to delete challenge vote")
                                .build()
                        );
                    }

                    return null;
                })));
    }
}
