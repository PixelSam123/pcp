package io.github.pixelsam123.pcp.user;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.unchecked.Unchecked;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/users")
public class UserResource {
    private final UserRepository userRepository;

    public UserResource(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<UserBriefDto> createUser(UserCreateDto userToCreate) {
        return Uni
            .createFrom()
            .item(() -> userRepository.find("name", userToCreate.name()).count())
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
            .map(Unchecked.function(dbUserCount -> {
                if (dbUserCount > 0) {
                    throw new BadRequestException(
                        Response
                            .status(Response.Status.BAD_REQUEST)
                            .entity("User already exists")
                            .build()
                    );
                }

                return new User(userToCreate);
            }))
            .map(user -> {
                userRepository.persist(user);

                return new UserBriefDto(user.getName(), user.getId(), user.getPoints());
            })
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<List<UserBriefDto>> getUsers() {
        return Uni
            .createFrom()
            .item(() -> userRepository.findAll().project(UserBriefDto.class).list())
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    @GET
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<UserBriefDto> getUser(@PathParam("name") String name) {
        return Uni
            .createFrom()
            .item(
                () -> userRepository
                    .find("name", name)
                    .project(UserBriefDto.class)
                    .firstResultOptional()
            )
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
            .map(Unchecked.function(dbUser -> {
                if (dbUser.isEmpty()) {
                    throw new NotFoundException(
                        Response
                            .status(Response.Status.NOT_FOUND)
                            .entity("User not found")
                            .build()
                    );
                }

                return dbUser.get();
            }));
    }
}
