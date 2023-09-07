package io.github.pixelsam123.pcp.challenge.submission.vote;

import io.github.pixelsam123.pcp.HttpException;
import io.github.pixelsam123.pcp.Utils;
import io.github.pixelsam123.pcp.challenge.submission.ChallengeSubmissionRepository;
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

import java.util.List;

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
    @Produces(MediaType.WILDCARD)
    public Uni<Void> create(
        ChallengeSubmissionVoteCreateDto challengeSubmissionVoteToCreate,
        @Context SecurityContext ctx
    ) {
        Uni<Long> userIdRetrieval = userRepository
            .findIdByName(ctx.getUserPrincipal().getName())
            .map(dbUserId -> dbUserId.orElseThrow(() -> new HttpException(
                Response.Status.BAD_REQUEST,
                "User of your credentials doesn't exist"
            )));

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
                    throw new HttpException(
                        Response.Status.BAD_REQUEST,
                        "Submission doesn't exist"
                    );
                }

                if (dbChallengeSubmissionVoteCount > 0) {
                    throw new HttpException(
                        Response.Status.BAD_REQUEST,
                        "User already voted on this submission"
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
    public Uni<List<ChallengeSubmissionVoteDto>> getListBySubmissionId(
        @PathParam("submission_id") long submissionId
    ) {
        return challengeSubmissionVoteRepository.listByChallengeSubmissionId(submissionId);
    }

    @DELETE
    @RolesAllowed({"User"})
    @Path("/{id}")
    @Produces(MediaType.WILDCARD)
    public Uni<Void> delete(@PathParam("id") long id, @Context SecurityContext ctx) {
        Uni<Long> userIdRetrieval =
            userRepository
                .findIdByName(ctx.getUserPrincipal().getName())
                .map(dbUserId -> dbUserId.orElseThrow(() -> new HttpException(
                    Response.Status.BAD_REQUEST,
                    "User of your credentials doesn't exist"
                )));

        Uni<Long> challengeSubmissionVoteUserIdRetrieval = challengeSubmissionVoteRepository
            .findUserIdById(id)
            .map(dbChallengeSubmissionVoteUserId -> dbChallengeSubmissionVoteUserId.orElseThrow(
                () -> new HttpException(Response.Status.NOT_FOUND, "Submission Vote Not Found")
            ));

        return Uni
            .combine()
            .all()
            .unis(userIdRetrieval, challengeSubmissionVoteUserIdRetrieval)
            .asTuple()
            .map(Utils::areItemsEqual)
            .flatMap(Unchecked.function(areIdsEqual -> {
                if (!areIdsEqual) {
                    throw new HttpException(
                        Response.Status.FORBIDDEN,
                        "Not allowed to delete on another user's behalf"
                    );
                }

                return challengeSubmissionVoteRepository.deleteById(id);
            }));
    }
}
