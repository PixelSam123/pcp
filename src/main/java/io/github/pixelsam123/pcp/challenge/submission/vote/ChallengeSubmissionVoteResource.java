package io.github.pixelsam123.pcp.challenge.submission.vote;

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
import java.util.Optional;

@Tag(
    name = "challenge_submission_votes",
    description = "Challenge submission vote creation, viewing and editing"
)
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
    @Transactional
    public Uni<Void> createChallengeSubmissionVote(
        ChallengeSubmissionVoteCreateDto challengeSubmissionVoteToCreate,
        @Context SecurityContext ctx
    ) {
        Uni<Long> userIdRetrieval = userRepository
            .findIdByName(ctx.getUserPrincipal().getName())
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

        Uni<Long> challengeSubmissionCountRetrieval =
            challengeSubmissionRepository.countById(challengeSubmissionVoteToCreate.submissionId());

        Uni<Long> challengeSubmissionVoteCountRetrieval = userIdRetrieval.flatMap(
            dbUserId -> challengeSubmissionVoteRepository.countByChallengeSubmissionIdAndUserId(
                challengeSubmissionVoteToCreate.submissionId(), dbUserId
            )
        );

        return Uni
            .combine()
            .all()
            .unis(
                userIdRetrieval,
                challengeSubmissionCountRetrieval,
                challengeSubmissionVoteCountRetrieval
            )
            .asTuple()
            .flatMap(Unchecked.function((tuple) -> {
                long dbUserId = tuple.getItem1();
                long dbChallengeSubmissionCount = tuple.getItem2();
                long dbChallengeSubmissionVoteCount = tuple.getItem3();

                if (dbChallengeSubmissionCount == 0) {
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

                return challengeSubmissionVoteRepository.persist(
                    challengeSubmissionVoteToCreate,
                    dbUserId
                );
            }));
    }

    @GET
    @Path("/{submission_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<List<ChallengeSubmissionVoteDto>> getChallengeSubmissionVotesBySubmissionId(
        @PathParam("submission_id") long submissionId
    ) {
        return challengeSubmissionVoteRepository.listByChallengeSubmissionId(submissionId);
    }

    @DELETE
    @RolesAllowed({"User"})
    @Path("/{id}")
    @Transactional
    public Uni<Void> deleteSubmissionVote(@PathParam("id") long id, @Context SecurityContext ctx) {
        Uni<Optional<Long>> userIdRetrieval =
            userRepository.findIdByName(ctx.getUserPrincipal().getName());

        Uni<Optional<Long>> challengeSubmissionVoteUserIdRetrieval =
            challengeSubmissionVoteRepository.findUserIdById(id);

        return Uni
            .combine()
            .all()
            .unis(userIdRetrieval, challengeSubmissionVoteUserIdRetrieval)
            .asTuple()
            .flatMap(Unchecked.function(tuple -> {
                Optional<Long> dbUserId = tuple.getItem1();
                Optional<Long> dbChallengeSubmissionVoteUserId = tuple.getItem2();

                if (dbUserId.isEmpty()) {
                    throw new BadRequestException(
                        Response
                            .status(Response.Status.BAD_REQUEST)
                            .entity("User of your credentials doesn't exist")
                            .build()
                    );
                }

                if (dbChallengeSubmissionVoteUserId.isEmpty()) {
                    throw new NotFoundException(
                        Response
                            .status(Response.Status.NOT_FOUND)
                            .entity("Submission Vote Not Found")
                            .build()
                    );
                }

                if (!dbUserId.get().equals(dbChallengeSubmissionVoteUserId.get())) {
                    throw new ForbiddenException(
                        Response
                            .status(Response.Status.FORBIDDEN)
                            .entity("Not allowed to delete on another user's behalf")
                            .build()
                    );
                }

                return challengeSubmissionVoteRepository.deleteById(id);
            }));
    }
}
