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
        Uni<Long> existingUserIdRetrieval = userRepository
            .findIdByName(ctx.getUserPrincipal().getName())
            .map(Unchecked.function(dbUser -> {
                if (dbUser.isEmpty()) {
                    throw new BadRequestException("User of your credentials doesn't exist");
                }

                return dbUser.get();
            }));

        Uni<Long> challengeSubmissionCountRetrieval =
            challengeSubmissionRepository.countById(challengeSubmissionVoteToCreate.submissionId());

        Uni<Long> challengeSubmissionVoteCountRetrieval = existingUserIdRetrieval.flatMap(
            existingDbUserId -> challengeSubmissionVoteRepository
                .countByChallengeSubmissionIdAndUserId(
                    challengeSubmissionVoteToCreate.submissionId(), existingDbUserId
                )
        );

        return Uni
            .combine()
            .all()
            .unis(
                existingUserIdRetrieval,
                challengeSubmissionCountRetrieval,
                challengeSubmissionVoteCountRetrieval
            )
            .asTuple()
            .flatMap(Unchecked.function((tuple) -> {
                long existingDbUserId = tuple.getItem1();
                long dbChallengeSubmissionCount = tuple.getItem2();
                long dbChallengeSubmissionVoteCount = tuple.getItem3();

                if (dbChallengeSubmissionCount == 0) {
                    throw new BadRequestException("Submission doesn't exist");
                }

                if (dbChallengeSubmissionVoteCount > 0) {
                    throw new BadRequestException("User already voted on this submission");
                }

                return challengeSubmissionVoteRepository.persist(
                    challengeSubmissionVoteToCreate,
                    existingDbUserId
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
                    throw new BadRequestException("User of your credentials doesn't exist");
                }

                if (dbChallengeSubmissionVoteUserId.isEmpty()) {
                    throw new NotFoundException("Submission Vote Not Found");
                }

                if (!dbUserId.get().equals(dbChallengeSubmissionVoteUserId.get())) {
                    throw new ForbiddenException("Not allowed to delete on another user's behalf");
                }

                return challengeSubmissionVoteRepository.deleteById(id);
            }));
    }
}
