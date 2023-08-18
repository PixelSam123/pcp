package io.github.pixelsam123.pcp.challenge.comment;

import io.github.pixelsam123.pcp.challenge.Challenge;
import io.github.pixelsam123.pcp.challenge.ChallengeRepository;
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

import java.util.List;
import java.util.Optional;

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
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Uni<ChallengeCommentDto> createChallengeComment(
        ChallengeCommentCreateDto challengeCommentToCreate, @Context SecurityContext ctx
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

        Uni<Optional<Challenge>> challengeRetrieval = Uni
            .createFrom()
            .item(() -> challengeRepository.findByIdOptional(challengeCommentToCreate.challengeId()))
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());

        return Uni
            .combine()
            .all()
            .unis(existingUserRetrieval, challengeRetrieval)
            .combinedWith(Unchecked.function((existingDbUser, dbChallenge) -> {
                if (dbChallenge.isEmpty()) {
                    throw new BadRequestException(
                        Response
                            .status(Response.Status.BAD_REQUEST)
                            .entity("Challenge doesn't exist")
                            .build()
                    );
                }

                return new ChallengeComment(
                    challengeCommentToCreate,
                    existingDbUser,
                    dbChallenge.get()
                );
            }))
            .map(challengeComment -> {
                challengeCommentRepository.persist(challengeComment);

                return new ChallengeCommentDto(challengeComment);
            })
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    @GET
    @Path("/{challenge_name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<List<ChallengeCommentDto>> getChallengeCommentsByChallengeName(
        @PathParam("challenge_name") String challengeName
    ) {
        Uni<Long> challengeIdRetrieval = Uni
            .createFrom()
            .item(() -> challengeRepository
                .find("name", challengeName)
                .firstResultOptional()
                .map(Challenge::getId))
            .map(Unchecked.function(dbChallengeId -> {
                if (dbChallengeId.isEmpty()) {
                    throw new NotFoundException(
                        Response
                            .status(Response.Status.NOT_FOUND)
                            .entity("Challenge Not Found")
                            .build()
                    );
                }

                return dbChallengeId.get();
            }));

        return challengeIdRetrieval
            .map(existingDbChallengeId -> challengeCommentRepository
                .find("challengeId", existingDbChallengeId)
                .project(ChallengeCommentDto.class)
                .list());
    }
}
