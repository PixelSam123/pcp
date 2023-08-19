package io.github.pixelsam123.pcp.challenge.submission;

import io.github.pixelsam123.pcp.challenge.Challenge;
import io.github.pixelsam123.pcp.challenge.ChallengeRepository;
import io.github.pixelsam123.pcp.challenge.vote.ChallengeVote;
import io.github.pixelsam123.pcp.challenge.vote.ChallengeVoteDto;
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

import java.util.List;

@Tag(ref = "challenge_submissions")
@Path("/challenge_submissions")
public class ChallengeSubmissionResource {
    private final ChallengeRepository challengeRepository;
    private final ChallengeSubmissionRepository challengeSubmissionRepository;
    private final UserRepository userRepository;

    public ChallengeSubmissionResource(
        ChallengeRepository challengeRepository,
        ChallengeSubmissionRepository challengeSubmissionRepository,
        UserRepository userRepository
    ) {
        this.challengeRepository = challengeRepository;
        this.challengeSubmissionRepository = challengeSubmissionRepository;
        this.userRepository = userRepository;
    }

    @POST
    @RolesAllowed({"User"})
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Uni<ChallengeSubmissionDto> createChallengeSubmission(
        ChallengeSubmissionCreateDto challengeSubmissionToCreate,
        @Context SecurityContext ctx
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

        Uni<Challenge> existingChallengeRetrieval = Uni
            .createFrom()
            .item(() -> challengeRepository.findByIdOptional(challengeSubmissionToCreate.challengeId()))
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
            .map(Unchecked.function(dbChallenge -> {
                if (dbChallenge.isEmpty()) {
                    throw new BadRequestException(
                        Response
                            .status(Response.Status.BAD_REQUEST)
                            .entity("Challenge doesn't exist")
                            .build()
                    );
                }

                return dbChallenge.get();
            }));

        Uni<Long> challengeSubmissionCountRetrieval = existingUserRetrieval
            .map(existingDbUser -> challengeSubmissionRepository
                .find(
                    "userId = ?1 and challengeId = ?2",
                    existingDbUser.getId(),
                    challengeSubmissionToCreate.challengeId()
                )
                .count())
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());

        return Uni
            .combine()
            .all()
            .unis(
                existingUserRetrieval,
                existingChallengeRetrieval,
                challengeSubmissionCountRetrieval
            )
            .asTuple()
            .map(Unchecked.function((tuple) -> {
                User existingDbUser = tuple.getItem1();
                Challenge existingDbChallenge = tuple.getItem2();
                long dbChallengeSubmissionCount = tuple.getItem3();

                if (dbChallengeSubmissionCount < 1) {
                    existingDbUser.setPoints(
                        existingDbUser.getPoints() + pointsForTier(existingDbChallenge.getTier())
                    );
                }

                ChallengeSubmission challengeSubmission = new ChallengeSubmission(
                    challengeSubmissionToCreate,
                    existingDbUser,
                    existingDbChallenge
                );
                challengeSubmissionRepository.persist(challengeSubmission);

                return new ChallengeSubmissionDto(challengeSubmission);
            }))
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    @GET
    @Path("/{challenge_name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<List<ChallengeSubmissionDto>> getChallengeSubmissionsByChallengeName(
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
            .map(existingDbChallengeId -> challengeSubmissionRepository
                .find("challengeId", existingDbChallengeId)
                .project(ChallengeSubmissionDto.class)
                .list());
    }

    private int pointsForTier(int tier) {
        return (6 - tier) * (6 - tier);
    }
}
