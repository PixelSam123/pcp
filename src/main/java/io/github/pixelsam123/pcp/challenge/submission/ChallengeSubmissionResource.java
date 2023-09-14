package io.github.pixelsam123.pcp.challenge.submission;

import io.github.pixelsam123.pcp.challenge.ChallengeRepository;
import io.github.pixelsam123.pcp.challenge.ChallengeVerifierView;
import io.github.pixelsam123.pcp.code.exec.CodeExecRequest;
import io.github.pixelsam123.pcp.code.exec.CodeExecResponse;
import io.github.pixelsam123.pcp.code.exec.CodeExecService;
import io.github.pixelsam123.pcp.common.ErrorMessages;
import io.github.pixelsam123.pcp.common.HttpException;
import io.github.pixelsam123.pcp.common.NotFoundException;
import io.github.pixelsam123.pcp.user.UserRepository;
import io.smallrye.mutiny.Uni;
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
    name = "challenge-submissions",
    description = "Challenge submission creation, viewing and editing"
)
@Path("/challenge-submissions")
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
        ChallengeSubmissionCreateDto challengeSubmission,
        @Context SecurityContext ctx
    ) {
        Uni<Long> userIdRetrieval = userRepository
            .findIdByName(ctx.getUserPrincipal().getName())
            .map(dbUser -> dbUser.orElseThrow(() -> new HttpException(
                Response.Status.BAD_REQUEST,
                ErrorMessages.CREDENTIALS_MISMATCH
            )));

        Uni<ChallengeVerifierView> challengeVerifierRetrieval = challengeRepository
            .findVerifierById(challengeSubmission.challengeId())
            .map(dbChallengeVerifier -> dbChallengeVerifier.orElseThrow(
                () -> new HttpException(Response.Status.BAD_REQUEST, "Challenge doesn't exist")
            ));

        Uni<Long> challengeSubmissionCountRetrieval = userIdRetrieval.flatMap(
            dbUserId -> challengeSubmissionRepository.countByChallengeIdAndUserId(
                challengeSubmission.challengeId(), dbUserId
            )
        );

        Uni<CodeExecResponse> codeExecRetrieval = challengeVerifierRetrieval
            .flatMap(dbChallengeVerifier -> codeExecService.getCodeExecResult(new CodeExecRequest(
                "js",
                challengeSubmission.code() + '\n' + dbChallengeVerifier.testCase()
            )));

        return Uni
            .combine()
            .all()
            .unis(
                userIdRetrieval,
                challengeVerifierRetrieval,
                challengeSubmissionCountRetrieval,
                codeExecRetrieval
            )
            .asTuple()
            .flatMap(Unchecked.function(tuple -> {
                long dbUserId = tuple.getItem1();
                ChallengeVerifierView dbChallengeVerifier = tuple.getItem2();
                long dbChallengeSubmissionCount = tuple.getItem3();
                CodeExecResponse codeExec = tuple.getItem4();

                if (codeExec.status() != 0) {
                    throw new HttpException(
                        Response.Status.BAD_REQUEST,
                        "Submission code execution error:\n" + codeExec.output()
                    );
                }

                Uni<Void> pointsAdditionTask =
                    dbChallengeSubmissionCount < 1 ? userRepository.addPointsById(
                        dbUserId,
                        pointsForTier(dbChallengeVerifier.tier())
                    ) : Uni.createFrom().voidItem();

                Uni<Void> challengeCompletedCountAdditionTask =
                    dbChallengeSubmissionCount < 1 ? challengeRepository.addCompletedCountById(
                        challengeSubmission.challengeId()
                    ) : Uni.createFrom().voidItem();

                return Uni
                    .combine()
                    .all()
                    .unis(
                        challengeSubmissionRepository.persist(challengeSubmission, dbUserId),
                        pointsAdditionTask,
                        challengeCompletedCountAdditionTask
                    )
                    .discardItems();
            }));
    }

    @GET
    @Path("/challenge-name/{challengeName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<List<ChallengeSubmissionDto>> listByChallengeName(
        @PathParam("challengeName") String challengeName
    ) {
        Uni<Long> challengeIdRetrieval = challengeRepository
            .findIdByName(challengeName)
            .map(dbChallengeId -> dbChallengeId.orElseThrow(
                () -> new NotFoundException("Challenge")
            ));

        return challengeIdRetrieval.flatMap(challengeSubmissionRepository::listByChallengeId);
    }

    private int pointsForTier(int tier) {
        return (6 - tier) * (6 - tier);
    }
}
