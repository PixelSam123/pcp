package io.github.pixelsam123.pcp.challenge;

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
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.Optional;

@Tag(ref = "challenges")
@Path("/challenges")
public class ChallengeResource {
    private final ChallengeRepository challengeRepository;
    private final UserRepository userRepository;

    public ChallengeResource(
        ChallengeRepository challengeRepository, UserRepository userRepository
    ) {
        this.challengeRepository = challengeRepository;
        this.userRepository = userRepository;
    }

    @POST
    @RolesAllowed({"User"})
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Uni<ChallengeDto> createChallenge(
        ChallengeCreateDto challengeToCreate, @Context SecurityContext ctx
    ) {
        Uni<Optional<User>> userRetrieval = Uni
            .createFrom()
            .item(() -> userRepository
                .find("name", ctx.getUserPrincipal().getName())
                .singleResultOptional())
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());

        Uni<Long> challengeCountRetrieval = Uni
            .createFrom()
            .item(() -> challengeRepository.find("name", challengeToCreate.name()).count())
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());

        return Uni
            .combine()
            .all()
            .unis(userRetrieval, challengeCountRetrieval)
            .combinedWith(Unchecked.function((dbUser, dbChallengeCount) -> {
                if (dbUser.isEmpty()) {
                    throw new BadRequestException(
                        Response
                            .status(Response.Status.BAD_REQUEST)
                            .entity("User of your credentials doesn't exist")
                            .build()
                    );
                }

                if (dbChallengeCount > 0) {
                    throw new BadRequestException(
                        Response
                            .status(Response.Status.BAD_REQUEST)
                            .entity("Challenge Already Exists")
                            .build()
                    );
                }

                return new Challenge(challengeToCreate, dbUser.get());
            }))
            .map(challenge -> {
                challengeRepository.persist(challenge);

                return new ChallengeDto(challenge);
            })
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<List<ChallengeBriefDto>> getChallenges() {
        return Uni
            .createFrom()
            .item(() -> challengeRepository.findAll().project(ChallengeBriefDto.class).list())
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    @GET
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<ChallengeDto> getChallengeByName(@PathParam("name") String name) {
        return Uni
            .createFrom()
            .item(() -> challengeRepository
                .find("name", name)
                .project(ChallengeDto.class)
                .firstResultOptional())
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
            .map(Unchecked.function(dbChallenge -> {
                if (dbChallenge.isEmpty()) {
                    throw new NotFoundException(
                        Response
                            .status(Response.Status.NOT_FOUND)
                            .entity("Challenge not found")
                            .build()
                    );
                }

                return dbChallenge.get();
            }));
    }
}