package io.github.pixelsam123.pcp.challenge.submission.comment;

import io.github.pixelsam123.pcp.challenge.submission.ChallengeSubmission;
import io.github.pixelsam123.pcp.challenge.submission.ChallengeSubmissionRepository;
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
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Uni<Void> createChallengeSubmissionComment(
        ChallengeSubmissionCommentCreateDto challengeSubmissionCommentToCreate,
        @Context SecurityContext ctx
    ) {
        Uni<Optional<Long>> userIdRetrieval =
            userRepository.findIdByName(ctx.getUserPrincipal().getName());

        Uni<Optional<ChallengeSubmission>> challengeSubmissionRetrieval =
            challengeSubmissionRepository.findById(challengeSubmissionCommentToCreate.submissionId());

        return Uni
            .combine()
            .all()
            .unis(userIdRetrieval, challengeSubmissionRetrieval)
            .asTuple()
            .flatMap(Unchecked.function(tuple -> {
                Optional<Long> dbUserId = tuple.getItem1();
                Optional<ChallengeSubmission> dbChallengeSubmission = tuple.getItem2();

                if (dbUserId.isEmpty()) {
                    throw new BadRequestException("User of your credentials doesn't exist");
                }

                if (dbChallengeSubmission.isEmpty()) {
                    throw new BadRequestException("Challenge submission doesn't exist");
                }

                return challengeSubmissionCommentRepository.persist(
                    challengeSubmissionCommentToCreate,
                    dbUserId.get()
                );
            }));
    }

    @GET
    @Path("/{challenge_submission_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<List<ChallengeSubmissionCommentDto>> getChallengeSubmissionCommentsByChallengeSubmissionId(
        @PathParam("challenge_submission_id") long challengeSubmissionId
    ) {
        return challengeSubmissionCommentRepository
            .listByChallengeSubmissionId(challengeSubmissionId);
    }
}
