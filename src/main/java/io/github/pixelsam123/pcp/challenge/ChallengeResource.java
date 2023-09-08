package io.github.pixelsam123.pcp.challenge;

import io.github.pixelsam123.pcp.HttpException;
import io.github.pixelsam123.pcp.Utils;
import io.github.pixelsam123.pcp.code.exec.CodeExecRequest;
import io.github.pixelsam123.pcp.code.exec.CodeExecResponse;
import io.github.pixelsam123.pcp.code.exec.CodeExecService;
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
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.Arrays;
import java.util.List;

import static java.lang.Boolean.FALSE;

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
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Void> create(
        ChallengeCreateDto challengeToCreate, @Context SecurityContext ctx
    ) {
        Uni<Long> userIdRetrieval = userRepository
            .findIdByName(ctx.getUserPrincipal().getName())
            .map(dbUserId -> dbUserId.orElseThrow(() -> new HttpException(
                Response.Status.BAD_REQUEST,
                "User of your credentials doesn't exist"
            )));

        Uni<Long> challengeCountRetrieval =
            challengeRepository.countByName(challengeToCreate.name());

        Uni<CodeExecResponse> codeExecRetrieval = codeExecService.getCodeExecResult(
            new CodeExecRequest(
                "js",
                challengeToCreate.codeForVerification() + '\n' + challengeToCreate.testCase()
            )
        );

        return Uni
            .combine()
            .all()
            .unis(userIdRetrieval, challengeCountRetrieval, codeExecRetrieval)
            .asTuple()
            .flatMap(Unchecked.function(tuple -> {
                long dbUserId = tuple.getItem1();
                long dbChallengeCount = tuple.getItem2();
                CodeExecResponse codeExec = tuple.getItem3();

                if (dbChallengeCount > 0) {
                    throw new HttpException(
                        Response.Status.BAD_REQUEST,
                        "Challenge Already Exists"
                    );
                }

                if (codeExec.status() != 0) {
                    throw new HttpException(
                        Response.Status.BAD_REQUEST,
                        "Verification code execution error:\n" + codeExec.output()
                    );
                }

                return challengeRepository.persist(challengeToCreate, dbUserId);
            }));
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<List<ChallengeBriefDto>> list(
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
    public Uni<ChallengeDto> getByName(@PathParam("name") String name) {
        return challengeRepository
            .findByNameDto(name)
            .map(dbChallenge -> dbChallenge.orElseThrow(
                () -> new HttpException(Response.Status.NOT_FOUND, "Challenge not found")
            ));
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
                "User of your credentials doesn't exist"
            )));

        Uni<Long> challengeUserIdRetrieval = challengeRepository
            .findUserIdById(id)
            .map(dbChallengeUserId -> dbChallengeUserId.orElseThrow(
                () -> new HttpException(Response.Status.NOT_FOUND, "Challenge Not Found")
            ));

        return Uni
            .combine()
            .all()
            .unis(userIdRetrieval, challengeUserIdRetrieval)
            .asTuple()
            .map(Utils::areItemsEqual)
            .flatMap(Unchecked.function(areIdsEqual -> {
                if (FALSE.equals(areIdsEqual)) {
                    throw new HttpException(
                        Response.Status.FORBIDDEN,
                        "Not allowed to delete on another user's behalf"
                    );
                }

                return challengeRepository.deleteById(id);
            }));
    }
}
