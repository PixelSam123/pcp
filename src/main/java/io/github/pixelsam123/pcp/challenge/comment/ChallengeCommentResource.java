package io.github.pixelsam123.pcp.challenge.comment;

import io.github.pixelsam123.pcp.HttpException;
import io.github.pixelsam123.pcp.challenge.ChallengeRepository;
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
    name = "challenge_comments",
    description = "Challenge comment creation, viewing and editing"
)
@Path("/challenge_comments")
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
    @Transactional
    public Uni<Void> create(
        ChallengeCommentCreateDto challengeCommentToCreate, @Context SecurityContext ctx
    ) {
        Uni<Optional<Long>> dbUserIdRetrieval =
            userRepository.findIdByName(ctx.getUserPrincipal().getName());

        Uni<Long> challengeCountRetrieval =
            challengeRepository.countById(challengeCommentToCreate.challengeId());

        return Uni
            .combine()
            .all()
            .unis(dbUserIdRetrieval, challengeCountRetrieval)
            .asTuple()
            .flatMap(Unchecked.function(tuple -> {
                Optional<Long> dbUserId = tuple.getItem1();
                long dbChallengeCount = tuple.getItem2();

                if (dbUserId.isEmpty()) {
                    throw new HttpException(
                        Response.Status.BAD_REQUEST,
                        "User of your credentials doesn't exist"
                    );
                }

                if (dbChallengeCount == 0) {
                    throw new HttpException(Response.Status.BAD_REQUEST, "Challenge doesn't exist");
                }

                return challengeCommentRepository.persist(challengeCommentToCreate, dbUserId.get());
            }));
    }

    @GET
    @Path("/{challenge_name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<List<ChallengeCommentDto>> getListByChallengeName(
        @PathParam("challenge_name") String challengeName
    ) {
        Uni<Long> challengeIdRetrieval = challengeRepository
            .findIdByName(challengeName)
            .map(Unchecked.function(dbChallenge -> {
                if (dbChallenge.isEmpty()) {
                    throw new HttpException(Response.Status.NOT_FOUND, "Challenge Not Found");
                }

                return dbChallenge.get();
            }));

        return challengeIdRetrieval.flatMap(challengeCommentRepository::listByChallengeId);
    }
}
