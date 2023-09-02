package io.github.pixelsam123.pcp.challenge.submission;

import io.github.pixelsam123.pcp.CodeExecRequest;
import io.github.pixelsam123.pcp.CodeExecResponse;
import io.github.pixelsam123.pcp.CodeExecService;
import io.github.pixelsam123.pcp.challenge.ChallengeRepository;
import io.github.pixelsam123.pcp.user.UserRepository;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import io.smallrye.mutiny.unchecked.Unchecked;
import jakarta.annotation.security.RolesAllowed;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.List;
import java.util.Optional;

@Tag(
    name = "challenge_submissions",
    description = "Challenge submission creation, viewing and editing"
)
@Path("/challenge_submissions")
public class ChallengeSubmissionResource {
    private final CodeExecService codeExecService;
    private final ChallengeRepository challengeRepository;
    private final ChallengeSubmissionRepository challengeSubmissionRepository;
    private final UserRepository userRepository;

    public ChallengeSubmissionResource(
        @RestClient CodeExecService codeExecService,
        ChallengeRepository challengeRepository,
        ChallengeSubmissionRepository challengeSubmissionRepository,
        UserRepository userRepository
    ) {
        this.codeExecService = codeExecService;
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

        Uni<Tuple2<Integer, String>> challengeTierAndTestCaseRetrieval =
            challengeRepository
                .findTierAndTestCaseById(challengeSubmissionToCreate.challengeId())
                .map(Unchecked.function(tuple -> {
                    if (tuple.isEmpty()) {
                        throw new BadRequestException("Challenge doesn't exist");
                    }

                    return tuple.get();
                }));

        Uni<Long> challengeSubmissionCountRetrieval = existingUserIdRetrieval.flatMap(
            existingDbUserId -> challengeSubmissionRepository.countByChallengeIdAndUserId(
                challengeSubmissionToCreate.challengeId(), existingDbUserId
            )
        );

        Uni<CodeExecResponse> codeExecRetrieval = challengeTierAndTestCaseRetrieval
            .flatMap(tuple -> codeExecService.getCodeExecResult(new CodeExecRequest(
                "js",
                challengeSubmissionToCreate.code() + '\n' + tuple.getItem2()
            )));

        return Uni
            .combine()
            .all()
            .unis(
                existingUserIdRetrieval,
                challengeTierAndTestCaseRetrieval,
                challengeSubmissionCountRetrieval,
                codeExecRetrieval
            )
            .asTuple()
            .flatMap(Unchecked.function((tuple) -> {
                long existingDbUserId = tuple.getItem1();
                Tuple2<Integer, String> dbChallengeTierAndTestCase = tuple.getItem2();
                long dbChallengeSubmissionCount = tuple.getItem3();
                CodeExecResponse codeExec = tuple.getItem4();

                if (codeExec.status() != 0) {
                    throw new BadRequestException(
                        Response
                            .status(Response.Status.BAD_REQUEST)
                            .entity("Submission code execution error:\n" + codeExec.output())
                            .build()
                    );
                }

                int challengeTier = dbChallengeTierAndTestCase.getItem1();

                Uni<Void> pointsAdditionTask =
                    dbChallengeSubmissionCount < 1 ? userRepository.addPointsById(
                        existingDbUserId,
                        pointsForTier(challengeTier)
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
