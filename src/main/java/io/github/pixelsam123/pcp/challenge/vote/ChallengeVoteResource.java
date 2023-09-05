package io.github.pixelsam123.pcp.challenge.vote;

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
    @Transactional
    public Uni<Void> create(
        ChallengeVoteCreateDto challengeVoteToCreate, @Context SecurityContext ctx
    ) {
        Uni<Long> userIdRetrieval = userRepository
            .findIdByName(ctx.getUserPrincipal().getName())
            .map(Unchecked.function(dbUserId -> {
                if (dbUserId.isEmpty()) {
                    throw new BadRequestException(
                        Response
                            .status(Response.Status.BAD_REQUEST)
                            .entity("User of your credentials doesn't exist")
                            .build()
                    );
                }

                return dbUserId.get();
            }));

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
                    throw new BadRequestException(
                        Response
                            .status(Response.Status.BAD_REQUEST)
                            .entity("Challenge doesn't exist")
                            .build()
                    );
                }

                if (dbChallengeVoteCount > 0) {
                    throw new BadRequestException(
                        Response
                            .status(Response.Status.BAD_REQUEST)
                            .entity("User already voted on this challenge")
                            .build()
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

        return challengeIdRetrieval.flatMap(challengeVoteRepository::listByChallengeId);
    }

    @DELETE
    @RolesAllowed({"User"})
    @Path("/{id}")
    @Transactional
    public Uni<Void> delete(@PathParam("id") long id, @Context SecurityContext ctx) {
        Uni<Optional<Long>> userIdRetrieval =
            userRepository.findIdByName(ctx.getUserPrincipal().getName());

        Uni<Optional<Long>> challengeVoteUserIdRetrieval =
            challengeVoteRepository.findUserIdById(id);

        return Uni
            .combine()
            .all()
            .unis(userIdRetrieval, challengeVoteUserIdRetrieval)
            .asTuple()
            .flatMap(Unchecked.function(tuple -> {
                Optional<Long> dbUserId = tuple.getItem1();
                Optional<Long> dbChallengeVoteUserId = tuple.getItem2();

                if (dbUserId.isEmpty()) {
                    throw new BadRequestException(
                        Response
                            .status(Response.Status.BAD_REQUEST)
                            .entity("User of your credentials doesn't exist")
                            .build()
                    );
                }

                if (dbChallengeVoteUserId.isEmpty()) {
                    throw new NotFoundException(
                        Response
                            .status(Response.Status.NOT_FOUND)
                            .entity("Challenge Vote Not Found")
                            .build()
                    );
                }

                if (!dbUserId.get().equals(dbChallengeVoteUserId.get())) {
                    throw new ForbiddenException(
                        Response
                            .status(Response.Status.FORBIDDEN)
                            .entity("Not allowed to delete on another user's behalf")
                            .build()
                    );
                }

                return challengeVoteRepository.deleteById(id);
            }));
    }
}
