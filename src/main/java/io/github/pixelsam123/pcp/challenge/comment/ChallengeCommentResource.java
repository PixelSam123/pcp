package io.github.pixelsam123.pcp.challenge.comment;

import io.github.pixelsam123.pcp.challenge.ChallengeRepository;
import io.github.pixelsam123.pcp.common.ErrorMessages;
import io.github.pixelsam123.pcp.common.HttpException;
import io.github.pixelsam123.pcp.common.NotFoundException;
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
    name = "challenge-comments",
    description = "Challenge comment creation, viewing and editing"
)
@Path("/challenge-comments")
public class ChallengeCommentResource {
    private final ChallengeRepository challengeRepository;
    private final ChallengeCommentRepository challengeCommentRepository;
    private final UserRepository userRepository;

    public ChallengeCommentResource(
        ChallengeRepository challengeRepository,
        ChallengeCommentRepository challengeCommentRepository,
        UserRepository userRepository
    ) {
        this.challengeRepository = challengeRepository;
        this.challengeCommentRepository = challengeCommentRepository;
        this.userRepository = userRepository;
    }

    @POST
    @RolesAllowed({"User"})
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Void> create(
        ChallengeCommentCreateDto challengeComment,
        @Context SecurityContext ctx
    ) {
        Uni<Long> dbUserIdRetrieval = userRepository
            .findIdByName(ctx.getUserPrincipal().getName())
            .map(dbUserId -> dbUserId.orElseThrow(() -> new HttpException(
                Response.Status.BAD_REQUEST,
                ErrorMessages.CREDENTIALS_MISMATCH
            )));

        Uni<Long> challengeCountRetrieval =
            challengeRepository.countById(challengeComment.challengeId());

        return Uni
            .combine()
            .all()
            .unis(dbUserIdRetrieval, challengeCountRetrieval)
            .asTuple()
            .flatMap(Unchecked.function(tuple -> {
                long dbUserId = tuple.getItem1();
                long dbChallengeCount = tuple.getItem2();

                if (dbChallengeCount == 0) {
                    throw new HttpException(Response.Status.BAD_REQUEST, "Challenge doesn't exist");
                }

                return challengeCommentRepository.persist(challengeComment, dbUserId);
            }));
    }

    @GET
    @Path("/challenge-name/{challengeName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<List<ChallengeCommentDto>> listByChallengeName(
        @PathParam("challengeName") String challengeName
    ) {
        Uni<Long> challengeIdRetrieval = challengeRepository
            .findIdByName(challengeName)
            .map(dbChallengeId -> dbChallengeId.orElseThrow(
                () -> new NotFoundException("Challenge")
            ));

        return challengeIdRetrieval.flatMap(challengeCommentRepository::listByChallengeId);
    }
}
