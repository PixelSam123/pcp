package io.github.pixelsam123.pcp.challenge.vote;

import io.github.pixelsam123.pcp.challenge.Challenge;
import io.github.pixelsam123.pcp.challenge.ChallengeRepository;
import io.github.pixelsam123.pcp.user.User;
import io.github.pixelsam123.pcp.user.UserRepository;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
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

@Tag(ref = "challenge_votes")
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
        Uni<User> existingUserRetrieval = Uni
            .createFrom()
            .item(() -> userRepository
                .find("name", ctx.getUserPrincipal().getName())
                .singleResultOptional())
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
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

        Uni<Optional<Challenge>> challengeRetrieval = Uni
            .createFrom()
            .item(() -> challengeRepository.findByIdOptional(challengeVoteToCreate.challengeId()))
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());

        Uni<Long> challengeVoteCountRetrieval = existingUserRetrieval
            .map(existingDbUser -> challengeVoteRepository
                .find(
                    "userId = ?1 and challengeId = ?2",
                    existingDbUser.getId(),
                    challengeVoteToCreate.challengeId()
                )
                .count())
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());

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
            .map(challengeVote -> {
                challengeVoteRepository.persist(challengeVote);

                return new ChallengeVoteDto(challengeVote);
            })
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    @GET
    @Path("/{challenge_name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<List<ChallengeVoteDto>> getChallengeVotesByChallengeName(
        @PathParam("challenge_name") String challengeName
    ) {
        Uni<Long> challengeIdRetrieval = Uni
            .createFrom()
            .item(() -> challengeRepository
                .find("name", challengeName)
                .firstResultOptional()
                .map(Challenge::getId))
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

        return challengeIdRetrieval
            .map(existingDbChallengeId -> challengeVoteRepository
                .find("challengeId", existingDbChallengeId)
                .project(ChallengeVoteDto.class)
                .list());
    }

    @DELETE
    @RolesAllowed({"User"})
    @Path("/{id}")
    @ResponseStatus(RestResponse.StatusCode.NO_CONTENT)
    public Uni<Void> deleteChallengeVote(@PathParam("id") long id, @Context SecurityContext ctx) {
        Uni<Optional<Long>> userIdRetrieval = Uni
            .createFrom()
            .item(() -> userRepository
                .find("name", ctx.getUserPrincipal().getName())
                .singleResultOptional()
                .map(User::getId))
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());

        Uni<Optional<Long>> challengeVoteIdRetrieval = Uni
            .createFrom()
            .item(() -> challengeVoteRepository
                .findByIdOptional(id)
                .map(ChallengeVote::getId))
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());

        return Uni
            .combine()
            .all()
            .unis(userIdRetrieval, challengeVoteIdRetrieval)
            .combinedWith(Unchecked.function((dbUserId, dbChallengeVoteId) -> {
                if (dbUserId.isEmpty()) {
                    throw new BadRequestException(
                        Response
                            .status(Response.Status.BAD_REQUEST)
                            .entity("User of your credentials doesn't exist")
                            .build()
                    );
                }

                if (dbChallengeVoteId.isEmpty()) {
                    throw new NotFoundException(
                        Response
                            .status(Response.Status.NOT_FOUND)
                            .entity("Challenge Vote Not Found")
                            .build()
                    );
                }

                long existingDbUserId = dbUserId.get();
                long existingDbChallengeVoteId = dbChallengeVoteId.get();

                if (existingDbUserId != existingDbChallengeVoteId) {
                    throw new ForbiddenException(
                        Response
                            .status(Response.Status.FORBIDDEN)
                            .entity("Not allowed to delete on another user's behalf")
                            .build()
                    );
                }

                return null;
            }))
            .map(unused -> {
                challengeVoteRepository.deleteById(id);

                return null;
            })
            .replaceWithVoid()
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }
}
