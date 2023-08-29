package io.github.pixelsam123.pcp.challenge.comment;

import io.github.pixelsam123.pcp.challenge.Challenge;
import io.github.pixelsam123.pcp.challenge.ChallengeRepository;
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
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Uni<Void> createChallengeComment(
        ChallengeCommentCreateDto challengeCommentToCreate, @Context SecurityContext ctx
    ) {
        Uni<Optional<User>> userRetrieval =
            userRepository.findByName(ctx.getUserPrincipal().getName());

        Uni<Optional<Challenge>> challengeRetrieval =
            challengeRepository.findById(challengeCommentToCreate.challengeId());

        return Uni
            .combine()
            .all()
            .unis(userRetrieval, challengeRetrieval)
            .asTuple()
            .flatMap(Unchecked.function(tuple -> {
                Optional<User> dbUser = tuple.getItem1();
                Optional<Challenge> dbChallenge = tuple.getItem2();

                if (dbUser.isEmpty()) {
                    throw new BadRequestException("User of your credentials doesn't exist");
                }

                if (dbChallenge.isEmpty()) {
                    throw new BadRequestException("Challenge doesn't exist");
                }

                return challengeCommentRepository.persist(challengeCommentToCreate, dbUser.get().id());
            }));
    }

    @GET
    @Path("/{challenge_name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<List<ChallengeCommentDto>> getChallengeCommentsByChallengeName(
        @PathParam("challenge_name") String challengeName
    ) {
        Uni<Long> challengeIdRetrieval = challengeRepository
            .findByName(challengeName)
            .map(Unchecked.function(dbChallenge -> {
                if (dbChallenge.isEmpty()) {
                    throw new NotFoundException("Challenge Not Found");
                }

                return dbChallenge.get().id();
            }));

        return challengeIdRetrieval.flatMap(challengeCommentRepository::listByChallengeId);
    }
}
