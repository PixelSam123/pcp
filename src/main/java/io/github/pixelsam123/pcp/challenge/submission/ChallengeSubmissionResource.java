package io.github.pixelsam123.pcp.challenge.submission;

import io.github.pixelsam123.pcp.HttpException;
import io.github.pixelsam123.pcp.challenge.ChallengeRepository;
import io.github.pixelsam123.pcp.code.exec.CodeExecRequest;
import io.github.pixelsam123.pcp.code.exec.CodeExecResponse;
import io.github.pixelsam123.pcp.code.exec.CodeExecService;
import io.github.pixelsam123.pcp.user.UserRepository;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import io.smallrye.mutiny.unchecked.Unchecked;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.List;

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
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Void> create(
        ChallengeSubmissionCreateDto challengeSubmissionToCreate,
        @Context SecurityContext ctx
    ) {
        Uni<Long> userIdRetrieval = userRepository
            .findIdByName(ctx.getUserPrincipal().getName())
            .map(dbUser -> dbUser.orElseThrow(() -> new HttpException(
                Response.Status.BAD_REQUEST,
                "User of your credentials doesn't exist"
            )));

        Uni<Tuple2<Integer, String>> challengeTierAndTestCaseRetrieval = challengeRepository
            .findTierAndTestCaseById(challengeSubmissionToCreate.challengeId())
            .map(challengeTierAndTestCase -> challengeTierAndTestCase.orElseThrow(
                () -> new HttpException(Response.Status.BAD_REQUEST, "Challenge doesn't exist")
            ));

        Uni<Long> challengeSubmissionCountRetrieval = userIdRetrieval.flatMap(
            dbUserId -> challengeSubmissionRepository.countByChallengeIdAndUserId(
                challengeSubmissionToCreate.challengeId(), dbUserId
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
                userIdRetrieval,
                challengeTierAndTestCaseRetrieval,
                challengeSubmissionCountRetrieval,
                codeExecRetrieval
            )
            .asTuple()
            .flatMap(Unchecked.function((tuple) -> {
                long dbUserId = tuple.getItem1();
                Tuple2<Integer, String> dbChallengeTierAndTestCase = tuple.getItem2();
                long dbChallengeSubmissionCount = tuple.getItem3();
                CodeExecResponse codeExec = tuple.getItem4();

                if (codeExec.status() != 0) {
                    throw new HttpException(
                        Response.Status.BAD_REQUEST,
                        "Submission code execution error:\n" + codeExec.output()
                    );
                }

                int challengeTier = dbChallengeTierAndTestCase.getItem1();

                Uni<Void> pointsAdditionTask =
                    dbChallengeSubmissionCount < 1 ? userRepository.addPointsById(
                        dbUserId,
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
                            dbUserId
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
    public Uni<List<ChallengeSubmissionDto>> getListByChallengeName(
        @PathParam("challenge_name") String challengeName
    ) {
        Uni<Long> challengeIdRetrieval = challengeRepository
            .findIdByName(challengeName)
            .map(dbChallengeId -> dbChallengeId.orElseThrow(
                () -> new HttpException(Response.Status.NOT_FOUND, "Challenge Not Found")
            ));

        return challengeIdRetrieval.flatMap(challengeSubmissionRepository::listByChallengeId);
    }

    private int pointsForTier(int tier) {
        return (6 - tier) * (6 - tier);
    }
}
