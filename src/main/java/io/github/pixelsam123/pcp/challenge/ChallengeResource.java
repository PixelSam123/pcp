package io.github.pixelsam123.pcp.challenge;

import io.github.pixelsam123.pcp.code.exec.CodeExecRequest;
import io.github.pixelsam123.pcp.code.exec.CodeExecResponse;
import io.github.pixelsam123.pcp.code.exec.CodeExecService;
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
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.Separator;

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
    public Uni<Void> create(ChallengeCreateDto challenge, @Context SecurityContext ctx) {
        Uni<Long> userIdRetrieval = userRepository
            .findIdByName(ctx.getUserPrincipal().getName())
            .map(dbUserId -> dbUserId.orElseThrow(() -> new HttpException(
                Response.Status.BAD_REQUEST,
                ErrorMessages.CREDENTIALS_MISMATCH
            )));

        Uni<Long> challengeCountRetrieval = challengeRepository.countByName(challenge.name());

        Uni<CodeExecResponse> codeExecRetrieval = codeExecService.getCodeExecResult(
            new CodeExecRequest(
                "js",
                challenge.codeForVerification() + '\n' + challenge.testCase()
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

                if (!challenge.name().matches("[a-zA-Z0-9 ]+")) {
                    throw new HttpException(
                        Response.Status.BAD_REQUEST,
                        "Challenge name must be alphanumeric or space"
                    );
                }

                if (codeExec.status() != 0) {
                    throw new HttpException(
                        Response.Status.BAD_REQUEST,
                        "Verification code execution error:\n" + codeExec.output()
                    );
                }

                return challengeRepository.persist(challenge, dbUserId);
            }));
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<List<ChallengeBriefDto>> list(
        @QueryParam("tiers") @Separator(",") List<Integer> tiers,
        @QueryParam("username") String username,
        @QueryParam("sort-by") ChallengeSort sortBy
    ) {
        return challengeRepository.list(
            tiers == null ? List.of(1, 2, 3, 4, 5) : tiers,
            username,
            sortBy == null ? ChallengeSort.NEWEST : sortBy
        );
    }

    @GET
    @Path("/name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<ChallengeDto> getByName(@PathParam("name") String name) {
        return challengeRepository
            .findDtoByName(name)
            .map(dbChallenge -> dbChallenge.orElseThrow(() -> new NotFoundException("Challenge")));
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"User"})
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Void> update(
        @PathParam("id") long id,
        ChallengeCreateDto challenge,
        @Context SecurityContext ctx
    ) {
        Uni<Long> userIdRetrieval = retrieveUserIdByUserName(ctx.getUserPrincipal().getName());
        Uni<Long> challengeUserIdRetrieval = retrieveChallengeUserIdByChallengeId(id);

        Uni<CodeExecResponse> codeExecRetrieval = codeExecService.getCodeExecResult(
            new CodeExecRequest(
                "js",
                challenge.codeForVerification() + '\n' + challenge.testCase()
            )
        );

        return Uni
            .combine()
            .all()
            .unis(userIdRetrieval, challengeUserIdRetrieval, codeExecRetrieval)
            .asTuple()
            .flatMap(Unchecked.function(tuple -> {
                long dbUserId = tuple.getItem1();
                long dbChallengeUserId = tuple.getItem2();
                CodeExecResponse codeExec = tuple.getItem3();

                if (dbUserId != dbChallengeUserId) {
                    throw new HttpException(
                        Response.Status.FORBIDDEN,
                        ErrorMessages.NO_EDIT_PERMISSION
                    );
                }

                if (codeExec.status() != 0) {
                    throw new HttpException(
                        Response.Status.BAD_REQUEST,
                        "Verification code execution error:\n" + codeExec.output()
                    );
                }

                return challengeRepository.updateById(challenge, id);
            }));
    }

    @DELETE
    @RolesAllowed({"User"})
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Void> delete(@PathParam("id") long id, @Context SecurityContext ctx) {
        Uni<Long> userIdRetrieval = retrieveUserIdByUserName(ctx.getUserPrincipal().getName());
        Uni<Long> challengeUserIdRetrieval = retrieveChallengeUserIdByChallengeId(id);

        return Utils
            .areUniItemsEqual(userIdRetrieval, challengeUserIdRetrieval)
            .flatMap(Unchecked.function(areIdsEqual -> {
                if (FALSE.equals(areIdsEqual)) {
                    throw new HttpException(
                        Response.Status.FORBIDDEN,
                        ErrorMessages.NO_DELETE_PERMISSION
                    );
                }

                return challengeRepository.deleteById(id);
            }));
    }

    private Uni<Long> retrieveUserIdByUserName(String userName) {
        return userRepository
            .findIdByName(userName)
            .map(dbUserId -> dbUserId.orElseThrow(() -> new HttpException(
                Response.Status.BAD_REQUEST,
                ErrorMessages.CREDENTIALS_MISMATCH
            )));
    }

    private Uni<Long> retrieveChallengeUserIdByChallengeId(long challengeId) {
        return challengeRepository
            .findUserIdById(challengeId)
            .map(dbChallengeUserId -> dbChallengeUserId.orElseThrow(
                () -> new NotFoundException("Challenge")
            ));
    }
}
