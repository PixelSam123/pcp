package io.github.pixelsam123.pcp.challenge.submission.vote;

import io.github.pixelsam123.pcp.challenge.submission.ChallengeSubmission;
import io.github.pixelsam123.pcp.challenge.submission.ChallengeSubmissionRepository;
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
import org.jboss.resteasy.reactive.ResponseStatus;
import org.jboss.resteasy.reactive.RestResponse;

import java.util.List;
import java.util.Optional;

@Tag(ref = "challenge_submission_votes")
@Path("/challenge_submission_votes")
public class ChallengeSubmissionVoteResource {
    private final ChallengeSubmissionRepository challengeSubmissionRepository;
    private final ChallengeSubmissionVoteRepository challengeSubmissionVoteRepository;
    private final UserRepository userRepository;

    public ChallengeSubmissionVoteResource(
        ChallengeSubmissionRepository challengeSubmissionRepository,
        ChallengeSubmissionVoteRepository challengeSubmissionVoteRepository,
        UserRepository userRepository
    ) {
        this.challengeSubmissionRepository = challengeSubmissionRepository;
        this.challengeSubmissionVoteRepository = challengeSubmissionVoteRepository;
        this.userRepository = userRepository;
    }

    @POST
    @RolesAllowed({"User"})
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Uni<ChallengeSubmissionVoteDto> createChallengeSubmissionVote(
        ChallengeSubmissionVoteCreateDto challengeSubmissionVoteToCreate,
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

        Uni<Optional<ChallengeSubmission>> challengeSubmissionRetrieval =
            challengeSubmissionRepository.asyncFindById(challengeSubmissionVoteToCreate.submissionId());

        Uni<Long> challengeSubmissionVoteCountRetrieval = existingUserRetrieval.flatMap(
            existingDbUser -> challengeSubmissionVoteRepository
                .asyncCountByChallengeSubmissionIdAndUserId(
                    challengeSubmissionVoteToCreate.submissionId(),
                    existingDbUser.getId()
                )
        );

        return Uni
            .combine()
            .all()
            .unis(
                existingUserRetrieval,
                challengeSubmissionRetrieval,
                challengeSubmissionVoteCountRetrieval
            )
            .asTuple()
            .map(Unchecked.function((tuple) -> {
                User existingDbUser = tuple.getItem1();
                Optional<ChallengeSubmission> dbChallengeSubmission = tuple.getItem2();
                long dbChallengeSubmissionVoteCount = tuple.getItem3();

                if (dbChallengeSubmission.isEmpty()) {
                    throw new BadRequestException(
                        Response
                            .status(Response.Status.BAD_REQUEST)
                            .entity("Submission doesn't exist")
                            .build()
                    );
                }

                if (dbChallengeSubmissionVoteCount > 0) {
                    throw new BadRequestException(
                        Response
                            .status(Response.Status.BAD_REQUEST)
                            .entity("User already voted on this submission")
                            .build()
                    );
                }

                return new ChallengeSubmissionVote(
                    challengeSubmissionVoteToCreate,
                    existingDbUser,
                    dbChallengeSubmission.get()
                );
            }))
            .flatMap(
                challengeSubmissionVote -> challengeSubmissionVoteRepository
                    .asyncPersist(challengeSubmissionVote)
                    .map((unused) -> new ChallengeSubmissionVoteDto(challengeSubmissionVote))
            );
    }

    @GET
    @Path("/{submission_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<List<ChallengeSubmissionVoteDto>> getChallengeSubmissionVotesBySubmissionId(
        @PathParam("submission_id") long submissionId
    ) {
        return challengeSubmissionVoteRepository.asyncListByChallengeSubmissionId(submissionId);
    }

    @DELETE
    @RolesAllowed({"User"})
    @Path("/{id}")
    @ResponseStatus(RestResponse.StatusCode.NO_CONTENT)
    public Uni<Void> deleteSubmissionVote(@PathParam("id") long id, @Context SecurityContext ctx) {
        Uni<Optional<Long>> userIdRetrieval = userRepository
            .asyncFindByName(ctx.getUserPrincipal().getName())
            .map(dbUser -> dbUser.map(User::getId));

        Uni<Optional<ChallengeSubmissionVote>> challengeSubmissionVoteRetrieval =
            challengeSubmissionVoteRepository.asyncFindById(id);

        return Uni
            .combine()
            .all()
            .unis(userIdRetrieval, challengeSubmissionVoteRetrieval)
            .combinedWith(Unchecked.function((dbUserId, dbChallengeSubmissionVote) -> {
                if (dbUserId.isEmpty()) {
                    throw new BadRequestException(
                        Response
                            .status(Response.Status.BAD_REQUEST)
                            .entity("User of your credentials doesn't exist")
                            .build()
                    );
                }

                if (dbChallengeSubmissionVote.isEmpty()) {
                    throw new NotFoundException(
                        Response
                            .status(Response.Status.NOT_FOUND)
                            .entity("Submission Vote Not Found")
                            .build()
                    );
                }

                Long existingDbUserId = dbUserId.get();
                ChallengeSubmissionVote existingDbSubmissionVote = dbChallengeSubmissionVote.get();

                if (!existingDbUserId.equals(existingDbSubmissionVote.getUser().getId())) {
                    throw new ForbiddenException(
                        Response
                            .status(Response.Status.FORBIDDEN)
                            .entity("Not allowed to delete on another user's behalf")
                            .build()
                    );
                }

                return null;
            }))
            .flatMap(unused -> challengeSubmissionVoteRepository
                .asyncDeleteById(id)
                .map(Unchecked.function((Boolean isDeleted) -> {
                    if (!isDeleted) {
                        throw new InternalServerErrorException(
                            Response
                                .status(Response.Status.INTERNAL_SERVER_ERROR)
                                .entity("Failed to delete challenge vote")
                                .build()
                        );
                    }

                    return null;
                })));
    }
}
