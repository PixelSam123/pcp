package io.github.pixelsam123.pcp.user;

import io.github.pixelsam123.pcp.HttpException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

import java.util.List;

@Tag(name = "users", description = "User creation, viewing and editing")
@Path("/users")
public class UserResource {
    private final Argon2PasswordEncoder argon2PasswordEncoder;
    private final UserRepository userRepository;

    public UserResource(
        Argon2PasswordEncoder argon2PasswordEncoder,
        UserRepository userRepository
    ) {
        this.argon2PasswordEncoder = argon2PasswordEncoder;
        this.userRepository = userRepository;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Void> create(UserCreateDto user) {
        return userRepository
            .countByName(user.name())
            .flatMap(Unchecked.function(dbUserCount -> {
                if (dbUserCount > 0) {
                    throw new HttpException(Response.Status.BAD_REQUEST, "User already exists");
                }

                return userRepository.persist(
                    user.name(),
                    argon2PasswordEncoder.encode(user.password())
                );
            }));
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<List<UserBriefDto>> list() {
        return userRepository.listAllBrief();
    }

    @GET
    @Path("/name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<UserBriefDto> getByName(@PathParam("name") String name) {
        return userRepository
            .findBriefByName(name)
            .map(dbUser -> dbUser.orElseThrow(
                () -> new HttpException(Response.Status.NOT_FOUND, "User not found")
            ));
    }
}
