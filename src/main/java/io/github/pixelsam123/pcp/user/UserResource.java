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

@Tag(ref = "users")
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
            .asyncCountByName(userToCreate.name())
            .map(Unchecked.function(dbUserCount -> {
                if (dbUserCount > 0) {
                    throw new BadRequestException(
                        Response
                            .status(Response.Status.BAD_REQUEST)
                            .entity("User already exists")
                            .build()
                    );
                }

                return new User(
                    userToCreate,
                    BcryptUtil.bcryptHash(userToCreate.password())
                );
            }))
            .flatMap(
                user -> userRepository.asyncPersist(user).map((unused) -> new UserBriefDto(user))
            );
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<List<UserBriefDto>> getUsers() {
        return userRepository.asyncListAllBrief();
    }

    @GET
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<UserBriefDto> getUserByName(@PathParam("name") String name) {
        return userRepository.asyncFindByNameBrief(name).map(
            Unchecked.function(dbUser -> {
                if (dbUser.isEmpty()) {
                    throw new NotFoundException(
                        Response
                            .status(Response.Status.NOT_FOUND)
                            .entity("User not found")
                            .build()
                    );
                }

                return dbUser.get();
            })
        );
    }
}
