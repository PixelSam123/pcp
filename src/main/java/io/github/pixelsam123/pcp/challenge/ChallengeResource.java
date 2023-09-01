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
                    throw new BadRequestException("User of your credentials doesn't exist");
                }

                if (dbChallengeCount > 0) {
                    throw new BadRequestException("Challenge Already Exists");
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
    public Uni<List<ChallengeBriefDto>> getChallenges() {
        return challengeRepository.listAllBrief();
    }

    @GET
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<ChallengeDto> getChallengeByName(@PathParam("name") String name) {
        return challengeRepository.findByNameDto(name).map(Unchecked.function(dbChallenge -> {
            if (dbChallenge.isEmpty()) {
                throw new NotFoundException("Challenge not found");
            }

            return dbChallenge.get();
        }));
    }
}
