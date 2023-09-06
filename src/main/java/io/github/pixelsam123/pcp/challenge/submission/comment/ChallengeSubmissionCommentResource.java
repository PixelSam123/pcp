package io.github.pixelsam123.pcp.challenge.submission.comment;

import io.github.pixelsam123.pcp.HttpException;
import io.github.pixelsam123.pcp.challenge.submission.ChallengeSubmissionRepository;
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
    name = "challenge_submission_comments",
    description = "Challenge submission comment creation, viewing and editing"
)
@Path("/challenge_submission_comments")
public class ChallengeSubmissionCommentResource {
    private final ChallengeSubmissionRepository challengeSubmissionRepository;
    private final ChallengeSubmissionCommentRepository challengeSubmissionCommentRepository;
    private final UserRepository userRepository;

    public ChallengeSubmissionCommentResource(
        ChallengeSubmissionRepository challengeSubmissionRepository,
        ChallengeSubmissionCommentRepository challengeSubmissionCommentRepository,
        UserRepository userRepository
    ) {
        this.challengeSubmissionRepository = challengeSubmissionRepository;
        this.challengeSubmissionCommentRepository = challengeSubmissionCommentRepository;
        this.userRepository = userRepository;
    }

    @POST
    @RolesAllowed({"User"})
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    public Uni<Void> create(
        ChallengeSubmissionCommentCreateDto challengeSubmissionCommentToCreate,
        @Context SecurityContext ctx
    ) {
        Uni<Long> userIdRetrieval = userRepository
            .findIdByName(ctx.getUserPrincipal().getName())
            .map(dbUserId -> dbUserId.orElseThrow(() -> new HttpException(
                Response.Status.BAD_REQUEST,
                "User of your credentials doesn't exist"
            )));

        Uni<Long> challengeSubmissionCountRetrieval = challengeSubmissionRepository.countById(
            challengeSubmissionCommentToCreate.submissionId()
        );

        return Uni
            .combine()
            .all()
            .unis(userIdRetrieval, challengeSubmissionCountRetrieval)
            .asTuple()
            .flatMap(Unchecked.function(tuple -> {
                long dbUserId = tuple.getItem1();
                long dbChallengeSubmissionCount = tuple.getItem2();

                if (dbChallengeSubmissionCount == 0) {
                    throw new HttpException(
                        Response.Status.BAD_REQUEST,
                        "Challenge submission doesn't exist"
                    );
                }

                return challengeSubmissionCommentRepository.persist(
                    challengeSubmissionCommentToCreate,
                    dbUserId
                );
            }));
    }

    @GET
    @Path("/{challenge_submission_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<List<ChallengeSubmissionCommentDto>> getListByChallengeSubmissionId(
        @PathParam("challenge_submission_id") long challengeSubmissionId
    ) {
        return challengeSubmissionCommentRepository.listByChallengeSubmissionId(
            challengeSubmissionId
        );
    }
}
