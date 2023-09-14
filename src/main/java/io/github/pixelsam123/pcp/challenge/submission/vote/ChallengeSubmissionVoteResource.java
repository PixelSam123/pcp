package io.github.pixelsam123.pcp.challenge.submission.vote;

import io.github.pixelsam123.pcp.challenge.submission.ChallengeSubmissionRepository;
import io.github.pixelsam123.pcp.common.ErrorMessages;
import io.github.pixelsam123.pcp.common.HttpException;
import io.github.pixelsam123.pcp.common.NotFoundException;
import io.github.pixelsam123.pcp.common.Utils;
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

import static java.lang.Boolean.FALSE;

@Tag(
    name = "challenge-submission-votes",
    description = "Challenge submission vote creation, viewing and editing"
)
@Path("/challenge-submission-votes")
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
    public Uni<Void> create(
        ChallengeSubmissionVoteCreateDto challengeSubmissionVote,
        @Context SecurityContext ctx
    ) {
        Uni<Long> userIdRetrieval = userRepository
            .findIdByName(ctx.getUserPrincipal().getName())
            .map(dbUserId -> dbUserId.orElseThrow(() -> new HttpException(
                Response.Status.BAD_REQUEST,
                ErrorMessages.CREDENTIALS_MISMATCH
            )));

        Uni<Long> challengeSubmissionCountRetrieval =
            challengeSubmissionRepository.countById(challengeSubmissionVote.challengeSubmissionId());

        Uni<Long> challengeSubmissionVoteCountRetrieval = userIdRetrieval.flatMap(
            dbUserId -> challengeSubmissionVoteRepository.countByChallengeSubmissionIdAndUserId(
                challengeSubmissionVote.challengeSubmissionId(), dbUserId
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
            .flatMap(Unchecked.function(tuple -> {
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

                return challengeSubmissionVoteRepository.persist(challengeSubmissionVote, dbUserId);
            }));
    }

    @GET
    @Path("/challenge-submission-id/{challengeSubmissionId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<List<ChallengeSubmissionVoteDto>> listByChallengeSubmissionId(
        @PathParam("challengeSubmissionId") long challengeSubmissionId
    ) {
        return challengeSubmissionVoteRepository.listByChallengeSubmissionId(challengeSubmissionId);
    }

    @DELETE
    @RolesAllowed({"User"})
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Void> delete(@PathParam("id") long id, @Context SecurityContext ctx) {
        Uni<Long> userIdRetrieval =
            userRepository
                .findIdByName(ctx.getUserPrincipal().getName())
                .map(dbUserId -> dbUserId.orElseThrow(() -> new HttpException(
                    Response.Status.BAD_REQUEST,
                    ErrorMessages.CREDENTIALS_MISMATCH
                )));

        Uni<Long> challengeSubmissionVoteUserIdRetrieval = challengeSubmissionVoteRepository
            .findUserIdById(id)
            .map(dbChallengeSubmissionVoteUserId -> dbChallengeSubmissionVoteUserId.orElseThrow(
                () -> new NotFoundException("Submission Vote")
            ));

        return Uni
            .combine()
            .all()
            .unis(userIdRetrieval, challengeSubmissionVoteUserIdRetrieval)
            .asTuple()
            .map(Utils::areItemsEqual)
            .flatMap(Unchecked.function(areIdsEqual -> {
                if (FALSE.equals(areIdsEqual)) {
                    throw new HttpException(
                        Response.Status.FORBIDDEN,
                        ErrorMessages.NO_DELETE_PERMISSION
                    );
                }

                return challengeSubmissionVoteRepository.deleteById(id);
            }));
    }
}
