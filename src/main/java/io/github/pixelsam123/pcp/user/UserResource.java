package io.github.pixelsam123.pcp.user;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Tag(name = "users", description = "User creation, viewing and editing")
@Path("/users")
public class UserResource {
    private final UserRepository userRepository;

    public UserResource(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Uni<UserBriefDto> createUser(UserCreateDto userToCreate) {
        return userRepository
            .countByName(userToCreate.name())
            .flatMap(Unchecked.function(dbUserCount -> {
                if (dbUserCount > 0) {
                    throw new BadRequestException("User already exists");
                }

                return userRepository.persist(
                    userToCreate.name(),
                    BcryptUtil.bcryptHash(userToCreate.password())
                );
            }))
            .flatMap(unused -> userRepository.findByNameBrief(userToCreate.name()))
            .map(Unchecked.function(dbUser -> {
                if (dbUser.isEmpty()) {
                    throw new InternalServerErrorException(
                        "Created user not found. Did creation fail?"
                    );
                }

                return dbUser.get();
            }));
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<List<UserBriefDto>> getUsers() {
        return userRepository.listAllBrief();
    }

    @GET
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<UserBriefDto> getUserByName(@PathParam("name") String name) {
        return userRepository.findByNameBrief(name).map(Unchecked.function(dbUser -> {
            if (dbUser.isEmpty()) {
                throw new NotFoundException("User not found");
            }

            return dbUser.get();
        }));
    }
}
