package io.github.pixelsam123.pcp.challenge.submission;

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
    @Transactional
    public Uni<Void> createChallengeSubmission(
        ChallengeSubmissionCreateDto challengeSubmissionToCreate,
        @Context SecurityContext ctx
    ) {
        Uni<Long> existingUserIdRetrieval = userRepository
            .findIdByName(ctx.getUserPrincipal().getName())
            .map(Unchecked.function(dbUser -> {
                if (dbUser.isEmpty()) {
                    throw new BadRequestException("User of your credentials doesn't exist");
                }

                return dbUser.get();
            }));

        Uni<Optional<Integer>> challengeTierRetrieval =
            challengeRepository.findTierById(challengeSubmissionToCreate.challengeId());

        Uni<Long> challengeSubmissionCountRetrieval = existingUserIdRetrieval.flatMap(
            existingDbUserId -> challengeSubmissionRepository.countByChallengeIdAndUserId(
                challengeSubmissionToCreate.challengeId(), existingDbUserId
            )
        );

        return Uni
            .combine()
            .all()
            .unis(
                existingUserIdRetrieval,
                challengeTierRetrieval,
                challengeSubmissionCountRetrieval
            )
            .asTuple()
            .flatMap(Unchecked.function((tuple) -> {
                long existingDbUserId = tuple.getItem1();
                Optional<Integer> dbChallengeTier = tuple.getItem2();
                long dbChallengeSubmissionCount = tuple.getItem3();

                if (dbChallengeTier.isEmpty()) {
                    throw new BadRequestException("Challenge doesn't exist");
                }

                Uni<Void> pointsAdditionTask =
                    dbChallengeSubmissionCount < 1 ? userRepository.addPointsById(
                        existingDbUserId,
                        pointsForTier(dbChallengeTier.get())
                    ) : Uni.createFrom().voidItem();

                Uni<Void> challengeCompletedCountAdditionTask =
                    dbChallengeSubmissionCount < 1 ? challengeRepository.addCompletedCountById(
                        challengeSubmissionToCreate.challengeId()
                    ) : Uni.createFrom().voidItem();

                return Uni
                    .combine()
                    .all()
                    .unis(
                        challengeSubmissionRepository.persist(
                            challengeSubmissionToCreate,
                            existingDbUserId
                        ),
                        pointsAdditionTask,
                        challengeCompletedCountAdditionTask
                    )
                    .asTuple();
            }))
            .replaceWithVoid();
    }

    @GET
    @Path("/{challenge_name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<List<ChallengeSubmissionDto>> getChallengeSubmissionsByChallengeName(
        @PathParam("challenge_name") String challengeName
    ) {
        Uni<Long> challengeIdRetrieval = challengeRepository
            .findIdByName(challengeName)
            .map(Unchecked.function(dbChallenge -> {
                if (dbChallenge.isEmpty()) {
                    throw new NotFoundException("Challenge Not Found");
                }

                return dbChallenge.get();
            }));

        return challengeIdRetrieval.flatMap(challengeSubmissionRepository::listByChallengeId);
    }

    private int pointsForTier(int tier) {
        return (6 - tier) * (6 - tier);
    }
}
