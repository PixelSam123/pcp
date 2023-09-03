package io.github.pixelsam123.pcp.challenge;

import io.github.pixelsam123.pcp.CodeExecRequest;
import io.github.pixelsam123.pcp.CodeExecResponse;
import io.github.pixelsam123.pcp.CodeExecService;
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
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Tag(name = "challenges", description = "Challenge creation, viewing and editing")
@Path("/challenges")
public class ChallengeResource {
    private final CodeExecService codeExecService;
    private final ChallengeRepository challengeRepository;
    private final UserRepository userRepository;

    public ChallengeResource(
        @RestClient CodeExecService codeExecService,
        ChallengeRepository challengeRepository,
        UserRepository userRepository
    ) {
        this.codeExecService = codeExecService;
        this.challengeRepository = challengeRepository;
        this.userRepository = userRepository;
    }

    @POST
    @RolesAllowed({"User"})
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    public Uni<Void> createChallenge(
        ChallengeCreateDto challengeToCreate, @Context SecurityContext ctx
    ) {
        Uni<Optional<Long>> userIdRetrieval =
            userRepository.findIdByName(ctx.getUserPrincipal().getName());

        Uni<Long> challengeCountRetrieval =
            challengeRepository.countByName(challengeToCreate.name());

        Uni<CodeExecResponse> codeExecRetrieval =
            codeExecService.getCodeExecResult(new CodeExecRequest(
                "js",
                challengeToCreate.codeForVerification() + '\n' + challengeToCreate.testCase()
            ));

        return Uni
            .combine()
            .all()
            .unis(userIdRetrieval, challengeCountRetrieval, codeExecRetrieval)
            .asTuple()
            .flatMap(Unchecked.function(tuple -> {
                Optional<Long> dbUserId = tuple.getItem1();
                long dbChallengeCount = tuple.getItem2();
                CodeExecResponse codeExec = tuple.getItem3();

                if (dbUserId.isEmpty()) {
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

                if (codeExec.status() != 0) {
                    throw new BadRequestException(
                        Response
                            .status(Response.Status.BAD_REQUEST)
                            .entity("Verification code execution error:\n" + codeExec.output())
                            .build()
                    );
                }

                return challengeRepository.persist(challengeToCreate, dbUserId.get());
            }));
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<List<ChallengeBriefDto>> getChallenges(
        @QueryParam("tiers") String tiers,
        @QueryParam("username") String username,
        @QueryParam("sortBy") String sortBy
    ) {
        if (tiers == null) tiers = "1,2,3,4,5";
        if (sortBy == null) sortBy = "newest";

        List<Integer> tierList = Arrays.stream(tiers.split(",")).map(Integer::parseInt).toList();
        ChallengeSort sort = switch (sortBy) {
            case "oldest" -> ChallengeSort.OLDEST;
            case "mostCompleted" -> ChallengeSort.MOST_COMPLETED;
            case "leastCompleted" -> ChallengeSort.LEAST_COMPLETED;
            default -> ChallengeSort.NEWEST;
        };

        return challengeRepository.list(tierList, username, sort);
    }

    @GET
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<ChallengeDto> getChallengeByName(@PathParam("name") String name) {
        return challengeRepository.findByNameDto(name).map(Unchecked.function(dbChallenge -> {
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

    @DELETE
    @RolesAllowed({"User"})
    @Path("/{id}")
    @Transactional
    public Uni<Void> deleteChallenge(@PathParam("id") long id, @Context SecurityContext ctx) {
        Uni<Optional<Long>> userIdRetrieval =
            userRepository.findIdByName(ctx.getUserPrincipal().getName());

        Uni<Optional<Long>> challengeUserIdRetrieval = challengeRepository.findUserIdById(id);

        return Uni
            .combine()
            .all()
            .unis(userIdRetrieval, challengeUserIdRetrieval)
            .asTuple()
            .flatMap(Unchecked.function(tuple -> {
                Optional<Long> dbUserId = tuple.getItem1();
                Optional<Long> dbChallengeUserId = tuple.getItem2();

                if (dbUserId.isEmpty()) {
                    throw new BadRequestException(
                        Response
                            .status(Response.Status.BAD_REQUEST)
                            .entity("User of your credentials doesn't exist")
                            .build()
                    );
                }

                if (dbChallengeUserId.isEmpty()) {
                    throw new NotFoundException(
                        Response
                            .status(Response.Status.NOT_FOUND)
                            .entity("Challenge Not Found")
                            .build()
                    );
                }

                if (!dbUserId.get().equals(dbChallengeUserId.get())) {
                    throw new ForbiddenException(
                        Response
                            .status(Response.Status.FORBIDDEN)
                            .entity("Not allowed to delete on another user's behalf")
                            .build()
                    );
                }

                return challengeRepository.deleteById(id);
            }));
    }
}
