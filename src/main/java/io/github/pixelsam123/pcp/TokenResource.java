package io.github.pixelsam123.pcp;

import io.github.pixelsam123.pcp.user.User;
import io.github.pixelsam123.pcp.user.UserRepository;
import io.smallrye.jwt.build.Jwt;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

import java.util.Map;

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
    public Map<String, String> loginForToken(String username, String password) {
        User dbUser = userRepository.find("name", username).singleResult();

        if (dbUser == null || !verifyPassword(password, dbUser.getPasswordHash())) {
            throw new BadRequestException(
                Response
                    .status(Response.Status.BAD_REQUEST)
                    .header("WWW-Authenticate", "Bearer")
                    .entity("Incorrect username or password")
                    .build()
            );
        }

        String token = createToken(dbUser.getId().toString(), dbUser.getName());

        return Map.ofEntries(Map.entry("access_token", token), Map.entry("token_type", "bearer"));
    }

    private boolean verifyPassword(String password, String hash) {
        return argon2PasswordEncoder.matches(password, hash);
    }

    private String createToken(String id, String username) {
        return Jwt.subject(id).upn(username).sign();
    }
}
