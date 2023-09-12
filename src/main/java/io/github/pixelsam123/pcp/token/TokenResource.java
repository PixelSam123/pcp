package io.github.pixelsam123.pcp.token;

import io.github.pixelsam123.pcp.common.HttpException;
import io.github.pixelsam123.pcp.user.UserRepository;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
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
    private final String cookieName;
    private final int tokenMinutesDuration;
    private final Argon2PasswordEncoder argon2PasswordEncoder;
    private final UserRepository userRepository;

    public TokenResource(
        @ConfigProperty(name = "mp.jwt.token.cookie") String cookieName,
        @ConfigProperty(name = "token-minutes-duration") int tokenMinutesDuration,
        Argon2PasswordEncoder argon2PasswordEncoder,
        UserRepository userRepository
    ) {
        this.cookieName = cookieName;
        this.tokenMinutesDuration = tokenMinutesDuration;
        this.argon2PasswordEncoder = argon2PasswordEncoder;
        this.userRepository = userRepository;
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> loginForToken(@RestForm String username, @RestForm String password) {
        return userRepository
            .findCredentialsByName(username)
            .map(dbCredentials -> dbCredentials.orElseThrow(
                () -> new HttpException(Response.Status.BAD_REQUEST, "Incorrect username")
            ))
            .map(Unchecked.function(dbCredentials -> {
                if (!verifyPassword(password, dbCredentials.passwordHash())) {
                    throw new HttpException(Response.Status.BAD_REQUEST, "Incorrect password");
                }

                String token = createToken(Long.toString(dbCredentials.id()), username);

                return Response
                    .ok(new Token(token, "Bearer"))
                    .cookie(new NewCookie.Builder(cookieName)
                        .value(token)
                        .domain(null)
                        .path("/")
                        .expiry(
                            Date.from(Instant.now().plus(Duration.ofMinutes(tokenMinutesDuration)))
                        )
                        .maxAge(tokenMinutesDuration * 60)
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
            .expiresIn(Duration.ofMinutes(tokenMinutesDuration))
            .sign();
    }
}
