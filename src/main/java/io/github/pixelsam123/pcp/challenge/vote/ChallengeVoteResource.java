package io.github.pixelsam123.pcp.challenge.vote;

import io.github.pixelsam123.pcp.HttpException;
import io.github.pixelsam123.pcp.Utils;
import io.github.pixelsam123.pcp.challenge.ChallengeRepository;
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

@Tag(name = "challenge_votes", description = "Challenge vote creation, viewing and editing")
@Path("/challenge_votes")
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
    @Produces(MediaType.WILDCARD)
    public Uni<Void> create(
        ChallengeVoteCreateDto challengeVoteToCreate, @Context SecurityContext ctx
    ) {
        Uni<Long> userIdRetrieval = userRepository
            .findIdByName(ctx.getUserPrincipal().getName())
            .map(dbUserId -> dbUserId.orElseThrow(() -> new HttpException(
                Response.Status.BAD_REQUEST,
                "User of your credentials doesn't exist"
            )));

        Uni<Long> challengeCountRetrieval =
            challengeRepository.countById(challengeVoteToCreate.challengeId());

        Uni<Long> challengeVoteCountRetrieval = userIdRetrieval.flatMap(
            dbUserId -> challengeVoteRepository.countByChallengeIdAndUserId(
                challengeVoteToCreate.challengeId(), dbUserId
            )
        );

        return Uni
            .combine()
            .all()
            .unis(userIdRetrieval, challengeCountRetrieval, challengeVoteCountRetrieval)
            .asTuple()
            .flatMap(Unchecked.function((tuple) -> {
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

                return challengeVoteRepository.persist(challengeVoteToCreate, dbUserId);
            }));
    }

    @GET
    @Path("/{challenge_name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<List<ChallengeVoteDto>> getListByChallengeName(
        @PathParam("challenge_name") String challengeName
    ) {
        Uni<Long> challengeIdRetrieval = challengeRepository
            .findIdByName(challengeName)
            .map(dbChallengeId -> dbChallengeId.orElseThrow(
                () -> new HttpException(Response.Status.NOT_FOUND, "Challenge Not Found")
            ));

        return challengeIdRetrieval.flatMap(challengeVoteRepository::listByChallengeId);
    }

    @DELETE
    @RolesAllowed({"User"})
    @Path("/{id}")
    @Produces(MediaType.WILDCARD)
    public Uni<Void> delete(@PathParam("id") long id, @Context SecurityContext ctx) {
        Uni<Long> userIdRetrieval = userRepository
            .findIdByName(ctx.getUserPrincipal().getName())
            .map(dbUserId -> dbUserId.orElseThrow(() -> new HttpException(
                Response.Status.BAD_REQUEST,
                "User of your credentials doesn't exist"
            )));

        Uni<Long> challengeVoteUserIdRetrieval = challengeVoteRepository
            .findUserIdById(id)
            .map(dbChallengeVoteUserId -> dbChallengeVoteUserId.orElseThrow(
                () -> new HttpException(Response.Status.NOT_FOUND, "Challenge Vote Not Found")
            ));

        return Uni
            .combine()
            .all()
            .unis(userIdRetrieval, challengeVoteUserIdRetrieval)
            .asTuple()
            .map(Utils::areItemsEqual)
            .flatMap(Unchecked.function(areIdsEqual -> {
                if (!areIdsEqual) {
                    throw new HttpException(
                        Response.Status.FORBIDDEN,
                        "Not allowed to delete on another user's behalf"
                    );
                }

                return challengeVoteRepository.deleteById(id);
            }));
    }
}
