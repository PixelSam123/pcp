package io.github.pixelsam123.pcp.challenge.submission.vote;

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
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.ResponseStatus;
import org.jboss.resteasy.reactive.RestResponse;

import java.util.List;
import java.util.Optional;

@Tag(ref = "submission_votes")
@Path("/submission_votes")
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
                challengeSubmissionVoteToCreate.submissionId()))
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());

        Uni<Long> challengeSubmissionVoteCountRetrieval = existingUserRetrieval
            .map(existingDbUser -> challengeSubmissionVoteRepository
                .find(
                    "userId = ?1 and submissionId = ?2",
                    existingDbUser.getId(),
                    challengeSubmissionVoteToCreate.submissionId()
                )
                .count())
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());

        return Uni
            .combine()
            .all()
            .unis(existingUserRetrieval, challengeSubmissionRetrieval, challengeSubmissionVoteCountRetrieval)
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
            .map(challengeSubmissionVote -> {
                challengeSubmissionVoteRepository.persist(challengeSubmissionVote);

                return new ChallengeSubmissionVoteDto(challengeSubmissionVote);
            })
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    @GET
    @Path("/{submission_name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<List<ChallengeSubmissionVoteDto>> getChallengeSubmissionVotesBySubmissionName(
        @PathParam("submission_name") String submissionName
    ) {
        Uni<Long> submissionIdRetrieval = Uni
            .createFrom()
            .item(() -> challengeSubmissionRepository
                .find("name", submissionName)
                .firstResultOptional()
                .map(ChallengeSubmission::getId))
            .map(Unchecked.function(dbSubmissionId -> {
                if (dbSubmissionId.isEmpty()) {
                    throw new NotFoundException(
                        Response
                            .status(Response.Status.NOT_FOUND)
                            .entity("Submission Not Found")
                            .build()
                    );
                }

                return dbSubmissionId.get();
            }));

        return submissionIdRetrieval
            .map(existingDbSubmissionId -> challengeSubmissionVoteRepository
                .find("submissionId", existingDbSubmissionId)
                .project(ChallengeSubmissionVoteDto.class)
                .list());
    }

    @DELETE
    @RolesAllowed({"User"})
    @Path("/{id}")
    @ResponseStatus(RestResponse.StatusCode.NO_CONTENT)
    public Uni<Void> deleteSubmissionVote(@PathParam("id") long id, @Context SecurityContext ctx) {
        Uni<Optional<Long>> userIdRetrieval = Uni
            .createFrom()
            .item(() -> userRepository
                .find("name", ctx.getUserPrincipal().getName())
                .singleResultOptional()
                .map(User::getId))
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());

        Uni<Optional<Long>> challengeSubmissionVoteIdRetrieval = Uni
            .createFrom()
            .item(() -> challengeSubmissionVoteRepository
                .findByIdOptional(id)
                .map(ChallengeSubmissionVote::getId))
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());

        return Uni
            .combine()
            .all()
            .unis(userIdRetrieval, challengeSubmissionVoteIdRetrieval)
            .combinedWith(Unchecked.function((dbUserId, dbChallengeSubmissionVoteId) -> {
                if (dbUserId.isEmpty()) {
                    throw new BadRequestException(
                        Response
                            .status(Response.Status.BAD_REQUEST)
                            .entity("User of your credentials doesn't exist")
                            .build()
                    );
                }

                if (dbChallengeSubmissionVoteId.isEmpty()) {
                    throw new NotFoundException(
                        Response
                            .status(Response.Status.NOT_FOUND)
                            .entity("Submission Vote Not Found")
                            .build()
                    );
                }

                long existingDbUserId = dbUserId.get();
                long existingDbSubmissionVoteId = dbChallengeSubmissionVoteId.get();

                if (existingDbUserId != existingDbSubmissionVoteId) {
                    throw new ForbiddenException(
                        Response
                            .status(Response.Status.FORBIDDEN)
                            .entity("Not allowed to delete on another user's behalf")
                            .build()
                    );
                }

                return null;
            }))
            .map(unused -> {
                challengeSubmissionVoteRepository.deleteById(id);

                return null;
            })
            .replaceWithVoid()
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }
}
