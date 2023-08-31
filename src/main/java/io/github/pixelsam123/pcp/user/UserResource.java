package io.github.pixelsam123.pcp.user;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
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
    @Transactional
    public Uni<Void> createUser(UserCreateDto userToCreate) {
        return userRepository
            .countByName(userToCreate.name())
            .flatMap(Unchecked.function(dbUserCount -> {
                if (dbUserCount > 0) {
                    throw new BadRequestException("User already exists");
                }

                return userRepository.persist(
                    userToCreate.name(),
                    argon2PasswordEncoder.encode(userToCreate.password())
                );
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
