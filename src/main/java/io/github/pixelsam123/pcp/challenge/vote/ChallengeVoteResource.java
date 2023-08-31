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
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

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
                    throw new NotFoundException("Challenge Not Found");
                }

                return dbChallengeId.get();
            }));

        return challengeIdRetrieval.flatMap(challengeVoteRepository::listByChallengeId);
    }

    @DELETE
    @RolesAllowed({"User"})
    @Path("/{id}")
    @Transactional
    public Uni<Void> deleteChallengeVote(@PathParam("id") long id, @Context SecurityContext ctx) {
        Uni<Optional<Long>> userIdRetrieval =
            userRepository.findIdByName(ctx.getUserPrincipal().getName());

        Uni<Optional<Long>> challengeVoteUserIdRetrieval =
            challengeVoteRepository.findUserIdById(id);

        return Uni
            .combine()
            .all()
            .unis(userIdRetrieval, challengeVoteUserIdRetrieval)
            .asTuple()
            .flatMap(Unchecked.function(tuple -> {
                Optional<Long> dbUserId = tuple.getItem1();
                Optional<Long> dbChallengeVoteUserId = tuple.getItem2();

                if (dbUserId.isEmpty()) {
                    throw new BadRequestException("User of your credentials doesn't exist");
                }

                if (dbChallengeVoteUserId.isEmpty()) {
                    throw new NotFoundException("Challenge Vote Not Found");
                }

                if (!dbUserId.get().equals(dbChallengeVoteUserId.get())) {
                    throw new ForbiddenException("Not allowed to delete on another user's behalf");
                }

                return challengeVoteRepository.deleteById(id);
            }));
    }
}
