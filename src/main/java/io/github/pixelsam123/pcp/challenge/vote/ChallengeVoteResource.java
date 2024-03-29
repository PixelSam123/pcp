package io.github.pixelsam123.pcp.challenge.vote;

import io.github.pixelsam123.pcp.challenge.ChallengeRepository;
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

@Tag(name = "challenge-votes", description = "Challenge vote creation, viewing and editing")
@Path("/challenge-votes")
public class ChallengeVoteResource {
    private final ChallengeRepository challengeRepository;
    private final ChallengeVoteRepository challengeVoteRepository;
    private final UserRepository userRepository;

    public ChallengeVoteResource(
        ChallengeRepository challengeRepository,
        ChallengeVoteRepository challengeVoteRepository,
        UserRepository userRepository
    ) {
        this.challengeRepository = challengeRepository;
        this.challengeVoteRepository = challengeVoteRepository;
        this.userRepository = userRepository;
    }

    @POST
    @RolesAllowed({"User"})
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Void> create(ChallengeVoteCreateDto challengeVote, @Context SecurityContext ctx) {
        Uni<Long> userIdRetrieval = userRepository
            .findIdByName(ctx.getUserPrincipal().getName())
            .map(dbUserId -> dbUserId.orElseThrow(() -> new HttpException(
                Response.Status.BAD_REQUEST,
                ErrorMessages.CREDENTIALS_MISMATCH
            )));

        Uni<Long> challengeCountRetrieval =
            challengeRepository.countById(challengeVote.challengeId());

        Uni<Long> challengeVoteCountRetrieval = userIdRetrieval.flatMap(
            dbUserId -> challengeVoteRepository.countByChallengeIdAndUserId(
                challengeVote.challengeId(), dbUserId
            )
        );

        return Uni
            .combine()
            .all()
            .unis(userIdRetrieval, challengeCountRetrieval, challengeVoteCountRetrieval)
            .asTuple()
            .flatMap(Unchecked.function(tuple -> {
                long dbUserId = tuple.getItem1();
                long dbChallengeCount = tuple.getItem2();
                long dbChallengeVoteCount = tuple.getItem3();

                if (dbChallengeCount == 0) {
                    throw new HttpException(Response.Status.BAD_REQUEST, "Challenge doesn't exist");
                }

                if (dbChallengeVoteCount > 0) {
                    throw new HttpException(
                        Response.Status.BAD_REQUEST,
                        "User already voted on this challenge"
                    );
                }

                return challengeVoteRepository.persist(challengeVote, dbUserId);
            }));
    }

    @GET
    @Path("/challenge-name/{challengeName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<List<ChallengeVoteDto>> listByChallengeName(
        @PathParam("challengeName") String challengeName
    ) {
        Uni<Long> challengeIdRetrieval = challengeRepository
            .findIdByName(challengeName)
            .map(dbChallengeId -> dbChallengeId.orElseThrow(
                () -> new NotFoundException("Challenge")
            ));

        return challengeIdRetrieval.flatMap(challengeVoteRepository::listByChallengeId);
    }

    @DELETE
    @RolesAllowed({"User"})
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Void> delete(@PathParam("id") long id, @Context SecurityContext ctx) {
        Uni<Long> userIdRetrieval = userRepository
            .findIdByName(ctx.getUserPrincipal().getName())
            .map(dbUserId -> dbUserId.orElseThrow(() -> new HttpException(
                Response.Status.BAD_REQUEST,
                ErrorMessages.CREDENTIALS_MISMATCH
            )));

        Uni<Long> challengeVoteUserIdRetrieval = challengeVoteRepository
            .findUserIdById(id)
            .map(dbChallengeVoteUserId -> dbChallengeVoteUserId.orElseThrow(
                () -> new NotFoundException("Challenge Vote")
            ));

        return Utils
            .areUniItemsEqual(userIdRetrieval, challengeVoteUserIdRetrieval)
            .flatMap(Unchecked.function(areIdsEqual -> {
                if (FALSE.equals(areIdsEqual)) {
                    throw new HttpException(
                        Response.Status.FORBIDDEN,
                        ErrorMessages.NO_DELETE_PERMISSION
                    );
                }

                return challengeVoteRepository.deleteById(id);
            }));
    }
}
