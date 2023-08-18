package io.github.pixelsam123.pcp.token;

import io.github.pixelsam123.pcp.user.User;
import io.github.pixelsam123.pcp.user.UserRepository;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.unchecked.Unchecked;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestForm;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

import java.util.Set;

@Path("/token")
public class TokenResource {
    private final Argon2PasswordEncoder argon2PasswordEncoder;
    private final UserRepository userRepository;

    public TokenResource(
        Argon2PasswordEncoder argon2PasswordEncoder,
        UserRepository userRepository
    ) {
        this.argon2PasswordEncoder = argon2PasswordEncoder;
        this.userRepository = userRepository;
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Token> loginForToken(@RestForm String username, @RestForm String password) {
        return Uni
            .createFrom()
            .item(() -> userRepository.find("name", username).firstResultOptional())
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
            .map(Unchecked.function(dbUser -> {
                if (dbUser.isEmpty()) {
                    throw new BadRequestException(
                        Response
                            .status(Response.Status.BAD_REQUEST)
                            .header("WWW-Authenticate", "Bearer")
                            .entity("Incorrect username")
                            .build()
                    );
                }

                User existingDbUser = dbUser.get();

                if (!verifyPassword(password, existingDbUser.getPasswordHash())) {
                    throw new BadRequestException(
                        Response
                            .status(Response.Status.BAD_REQUEST)
                            .header("WWW-Authenticate", "Bearer")
                            .entity("Incorrect password")
                            .build()
                    );
                }

                String token =
                    createToken(existingDbUser.getId().toString(), existingDbUser.getName());

                return new Token(token, "Bearer");
            }));
    }

    private boolean verifyPassword(String password, String hash) {
        return argon2PasswordEncoder.matches(password, hash);
    }

    private String createToken(String id, String username) {
        return Jwt.subject(id).upn(username).groups(Set.of("User")).sign();
    }
}
