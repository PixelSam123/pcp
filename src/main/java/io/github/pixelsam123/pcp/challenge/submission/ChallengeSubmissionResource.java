package io.github.pixelsam123.pcp.challenge.submission;

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

import java.util.List;

@Tag(
    name = "challenge_submissions",
    description = "Challenge submission creation, viewing and editing"
)
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
        Uni<User> existingUserRetrieval = userRepository
            .asyncFindByName(ctx.getUserPrincipal().getName())
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

        Uni<Challenge> existingChallengeRetrieval = challengeRepository
            .asyncFindById(challengeSubmissionToCreate.challengeId())
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

        Uni<Long> challengeSubmissionCountRetrieval = existingUserRetrieval.flatMap(
            existingDbUser -> challengeSubmissionRepository.asyncCountByChallengeIdAndUserId(
                challengeSubmissionToCreate.challengeId(), existingDbUser.getId()
            )
        );

        return Uni
            .combine()
            .all()
            .unis(
                existingUserRetrieval,
                existingChallengeRetrieval,
                challengeSubmissionCountRetrieval
            )
            .asTuple()
            .flatMap(Unchecked.function((tuple) -> {
                User existingDbUser = tuple.getItem1();
                Challenge existingDbChallenge = tuple.getItem2();
                long dbChallengeSubmissionCount = tuple.getItem3();

                ChallengeSubmission challengeSubmission = new ChallengeSubmission(
                    challengeSubmissionToCreate,
                    existingDbUser,
                    existingDbChallenge
                );

                Uni<Void> pointsAdditionTask =
                    dbChallengeSubmissionCount < 1 ? userRepository.asyncAddPoints(
                        existingDbUser,
                        pointsForTier(existingDbChallenge.getTier())
                    ) : Uni.createFrom().voidItem();

                Uni<Void> challengeCompletedCountAdditionTask =
                    dbChallengeSubmissionCount < 1 ? challengeRepository.asyncAddCompletedCount(
                        existingDbChallenge
                    ) : Uni.createFrom().voidItem();

                return Uni
                    .combine()
                    .all()
                    .unis(
                        challengeSubmissionRepository.asyncPersist(challengeSubmission),
                        pointsAdditionTask,
                        challengeCompletedCountAdditionTask
                    )
                    .asTuple()
                    .map(unused -> new ChallengeSubmissionDto(challengeSubmission));
            }));
    }

    @GET
    @Path("/{challenge_name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<List<ChallengeSubmissionDto>> getChallengeSubmissionsByChallengeName(
        @PathParam("challenge_name") String challengeName
    ) {
        Uni<Long> challengeIdRetrieval = challengeRepository
            .asyncFindByName(challengeName)
            .map(Unchecked.function(dbChallenge -> {
                if (dbChallenge.isEmpty()) {
                    throw new NotFoundException(
                        Response
                            .status(Response.Status.NOT_FOUND)
                            .entity("Challenge Not Found")
                            .build()
                    );
                }

                return dbChallenge.get().getId();
            }));

        return challengeIdRetrieval.flatMap(challengeSubmissionRepository::asyncListByChallengeId);
    }

    private int pointsForTier(int tier) {
        return (6 - tier) * (6 - tier);
    }
}
