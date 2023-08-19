package io.github.pixelsam123.pcp.challenge.submission.comment;

import io.github.pixelsam123.pcp.challenge.submission.ChallengeSubmission;
import io.github.pixelsam123.pcp.challenge.submission.ChallengeSubmissionRepository;
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

import java.util.List;
import java.util.Optional;

@Path("/challenge_comments")
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
    public Uni<ChallengeSubmissionCommentDto> createChallengeSubmissionComment(
        ChallengeSubmissionCommentCreateDto challengeSubmissionCommentToCreate,
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

        Uni<Optional<ChallengeSubmission>> challengeSubmissionRetrieval = Uni
            .createFrom()
            .item(() -> challengeSubmissionRepository.findByIdOptional(
                challengeSubmissionCommentToCreate.submissionId()
            ))
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());

        return Uni
            .combine()
            .all()
            .unis(existingUserRetrieval, challengeSubmissionRetrieval)
            .combinedWith(Unchecked.function((existingDbUser, dbChallengeSubmission) -> {
                if (dbChallengeSubmission.isEmpty()) {
                    throw new BadRequestException(
                        Response
                            .status(Response.Status.BAD_REQUEST)
                            .entity("Challenge submission doesn't exist")
                            .build()
                    );
                }

                return new ChallengeSubmissionComment(
                    challengeSubmissionCommentToCreate,
                    existingDbUser,
                    dbChallengeSubmission.get()
                );
            }))
            .map(challengeSubmissionComment -> {
                challengeSubmissionCommentRepository.persist(challengeSubmissionComment);

                return new ChallengeSubmissionCommentDto(challengeSubmissionComment);
            })
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    @GET
    @Path("/{challenge_name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<List<ChallengeSubmissionCommentDto>> getChallengeSubmissionCommentsByChallengeName(
        @PathParam("challenge_name") String challengeName
    ) {
        Uni<Long> challengeSubmissionIdRetrieval = Uni
            .createFrom()
            .item(() -> challengeSubmissionRepository
                .find("name", challengeName)
                .firstResultOptional()
                .map(ChallengeSubmission::getId))
            .map(Unchecked.function(dbChallengeSubmissionId -> {
                if (dbChallengeSubmissionId.isEmpty()) {
                    throw new NotFoundException(
                        Response
                            .status(Response.Status.NOT_FOUND)
                            .entity("Challenge submission Not Found")
                            .build()
                    );
                }

                return dbChallengeSubmissionId.get();
            }));

        return challengeSubmissionIdRetrieval
            .map(existingDbChallengeSubmissionId -> challengeSubmissionCommentRepository
                .find("challengeSubmissionId", existingDbChallengeSubmissionId)
                .project(ChallengeSubmissionCommentDto.class)
                .list());
    }
}
