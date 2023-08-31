package io.github.pixelsam123.pcp.token;

import io.github.pixelsam123.pcp.user.UserRepository;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.RestForm;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Set;

@Path("/token")
public class TokenResource {
    private final Argon2PasswordEncoder argon2PasswordEncoder;
    private final UserRepository userRepository;
    private final String tokenSecretKey;

    public TokenResource(
        Argon2PasswordEncoder argon2PasswordEncoder,
        UserRepository userRepository,
        @ConfigProperty(name = "token_secret_key") String tokenSecretKey
    ) {
        this.argon2PasswordEncoder = argon2PasswordEncoder;
        this.userRepository = userRepository;
        this.tokenSecretKey = tokenSecretKey;
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> loginForToken(@RestForm String username, @RestForm String password) {
        return userRepository
            .findIdAndPasswordHashByName(username)
            .map(Unchecked.function(tuple -> {
                if (tuple.isEmpty()) {
                    throw new BadRequestException("Incorrect username");
                }

                long existingDbUserId = tuple.get().getItem1();
                String existingDbUserPasswordHash = tuple.get().getItem2();

                if (!verifyPassword(password, existingDbUserPasswordHash)) {
                    throw new BadRequestException("Incorrect password");
                }

                String token = createToken(Long.toString(existingDbUserId), username);

                return Response
                    .ok(new Token(token, "Bearer"))
                    .cookie(new NewCookie.Builder("quarkus-credential")
                        .value(token)
                        .domain(null)
                        .path("/")
                        .expiry(Date.from(Instant.now().plus(Duration.ofMillis(60))))
                        .maxAge(60 * 3600)
                        .httpOnly(true)
                        .sameSite(NewCookie.SameSite.STRICT)
                        .build())
                    .build();
            }));
    }

    private boolean verifyPassword(String password, String hash) {
        return argon2PasswordEncoder.matches(password, hash);
    }

    private String createToken(String id, String username) {
        return Jwt
            .subject(id)
            .upn(username)
            .groups(Set.of("User"))
            .expiresIn(Duration.ofMinutes(60))
            .signWithSecret(tokenSecretKey);
    }
}
