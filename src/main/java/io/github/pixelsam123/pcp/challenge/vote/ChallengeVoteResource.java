package io.github.pixelsam123.pcp.challenge.vote;

import io.github.pixelsam123.pcp.challenge.ChallengeRepository;
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
    public Uni<Void> createChallengeVote(
        ChallengeVoteCreateDto challengeVoteToCreate, @Context SecurityContext ctx
    ) {
        Uni<Long> existingUserIdRetrieval = userRepository
            .findIdByName(ctx.getUserPrincipal().getName())
            .map(Unchecked.function(dbUser -> {
                if (dbUser.isEmpty()) {
                    throw new BadRequestException("User of your credentials doesn't exist");
                }

                return dbUser.get();
            }));

        Uni<Long> challengeCountRetrieval =
            challengeRepository.countById(challengeVoteToCreate.challengeId());

        Uni<Long> challengeVoteCountRetrieval = existingUserIdRetrieval.flatMap(
            existingDbUserId -> challengeVoteRepository.countByChallengeIdAndUserId(
                challengeVoteToCreate.challengeId(), existingDbUserId
            )
        );

        return Uni
            .combine()
            .all()
            .unis(existingUserIdRetrieval, challengeCountRetrieval, challengeVoteCountRetrieval)
            .asTuple()
            .flatMap(Unchecked.function((tuple) -> {
                long existingDbUserId = tuple.getItem1();
                long dbChallengeCount = tuple.getItem2();
                long dbChallengeVoteCount = tuple.getItem3();

                if (dbChallengeCount == 0) {
                    throw new BadRequestException("Challenge doesn't exist");
                }

                if (dbChallengeVoteCount > 0) {
                    throw new BadRequestException("User already voted on this challenge");
                }

                return challengeVoteRepository.persist(challengeVoteToCreate, existingDbUserId);
            }));
    }

    @GET
    @Path("/{challenge_name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<List<ChallengeVoteDto>> getChallengeVotesByChallengeName(
        @PathParam("challenge_name") String challengeName
    ) {
        Uni<Long> challengeIdRetrieval = challengeRepository
            .findIdByName(challengeName)
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

        return challengeIdRetrieval.flatMap(challengeVoteRepository::listByChallengeId);
    }

    @DELETE
    @RolesAllowed({"User"})
    @Path("/{id}")
    @ResponseStatus(RestResponse.StatusCode.NO_CONTENT)
    public Uni<Void> deleteChallengeVote(@PathParam("id") long id, @Context SecurityContext ctx) {
        Uni<Optional<Long>> userIdRetrieval = userRepository
            .findIdByName(ctx.getUserPrincipal().getName());

        Uni<Optional<ChallengeVote>> challengeVoteRetrieval = challengeVoteRepository.findById(id);

        return Uni
            .combine()
            .all()
            .unis(userIdRetrieval, challengeVoteRetrieval)
            .asTuple()
            .flatMap(Unchecked.function(tuple -> {
                Optional<Long> dbUserId = tuple.getItem1();
                Optional<ChallengeVote> dbChallengeVote = tuple.getItem2();

                if (dbUserId.isEmpty()) {
                    throw new BadRequestException("User of your credentials doesn't exist");
                }

                if (dbChallengeVote.isEmpty()) {
                    throw new NotFoundException("Challenge Vote Not Found");
                }

                Long existingDbUserId = dbUserId.get();
                ChallengeVote existingDbChallengeVote = dbChallengeVote.get();

                if (!existingDbUserId.equals(existingDbChallengeVote.user().id())) {
                    throw new ForbiddenException("Not allowed to delete on another user's behalf");
                }

                return challengeVoteRepository.deleteById(id);
            }));
    }
}
