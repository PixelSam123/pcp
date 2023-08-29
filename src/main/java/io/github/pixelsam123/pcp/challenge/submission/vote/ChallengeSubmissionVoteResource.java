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
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.ResponseStatus;
import org.jboss.resteasy.reactive.RestResponse;

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
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Uni<Void> createChallengeSubmissionVote(
        ChallengeSubmissionVoteCreateDto challengeSubmissionVoteToCreate,
        @Context SecurityContext ctx
    ) {
        Uni<User> existingUserRetrieval = userRepository
            .findByName(ctx.getUserPrincipal().getName())
            .map(Unchecked.function(dbUser -> {
                if (dbUser.isEmpty()) {
                    throw new BadRequestException("User of your credentials doesn't exist");
                }

                return dbUser.get();
            }));

        Uni<Optional<ChallengeSubmission>> challengeSubmissionRetrieval =
            challengeSubmissionRepository.findById(challengeSubmissionVoteToCreate.submissionId());

        Uni<Long> challengeSubmissionVoteCountRetrieval = existingUserRetrieval.flatMap(
            existingDbUser -> challengeSubmissionVoteRepository
                .countByChallengeSubmissionIdAndUserId(
                    challengeSubmissionVoteToCreate.submissionId(),
                    existingDbUser.id()
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
            .flatMap(Unchecked.function((tuple) -> {
                User existingDbUser = tuple.getItem1();
                Optional<ChallengeSubmission> dbChallengeSubmission = tuple.getItem2();
                long dbChallengeSubmissionVoteCount = tuple.getItem3();

                if (dbChallengeSubmission.isEmpty()) {
                    throw new BadRequestException("Submission doesn't exist");
                }

                if (dbChallengeSubmissionVoteCount > 0) {
                    throw new BadRequestException("User already voted on this submission");
                }

                return challengeSubmissionVoteRepository.persist(
                    challengeSubmissionVoteToCreate,
                    existingDbUser.id()
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
    @ResponseStatus(RestResponse.StatusCode.NO_CONTENT)
    public Uni<Void> deleteSubmissionVote(@PathParam("id") long id, @Context SecurityContext ctx) {
        Uni<Optional<Long>> userIdRetrieval = userRepository
            .findByName(ctx.getUserPrincipal().getName())
            .map(dbUser -> dbUser.map(User::id));

        Uni<Optional<ChallengeSubmissionVote>> challengeSubmissionVoteRetrieval =
            challengeSubmissionVoteRepository.findById(id);

        return Uni
            .combine()
            .all()
            .unis(userIdRetrieval, challengeSubmissionVoteRetrieval)
            .asTuple()
            .flatMap(Unchecked.function(tuple -> {
                Optional<Long> dbUserId = tuple.getItem1();
                Optional<ChallengeSubmissionVote> dbChallengeSubmissionVote = tuple.getItem2();

                if (dbUserId.isEmpty()) {
                    throw new BadRequestException("User of your credentials doesn't exist");
                }

                if (dbChallengeSubmissionVote.isEmpty()) {
                    throw new NotFoundException("Submission Vote Not Found");
                }

                Long existingDbUserId = dbUserId.get();
                ChallengeSubmissionVote existingDbSubmissionVote = dbChallengeSubmissionVote.get();

                if (!existingDbUserId.equals(existingDbSubmissionVote.user().id())) {
                    throw new ForbiddenException("Not allowed to delete on another user's behalf");
                }

                return challengeSubmissionVoteRepository.deleteById(id);
            }));
    }
}
