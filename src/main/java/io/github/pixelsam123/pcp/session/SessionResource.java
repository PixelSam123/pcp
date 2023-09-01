package io.github.pixelsam123.pcp.session;

import io.github.pixelsam123.pcp.user.UserRepository;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.util.Date;

@Path("/session")
public class SessionResource {
    private final String cookieName;
    private final UserRepository userRepository;

    public SessionResource(
        @ConfigProperty(name = "mp.jwt.token.cookie") String cookieName,
        UserRepository userRepository
    ) {
        this.cookieName = cookieName;
        this.userRepository = userRepository;
    }

    @GET
    @RolesAllowed({"User"})
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Session> sessionGet(@Context SecurityContext ctx) {
        return userRepository
            .findByNameBrief(ctx.getUserPrincipal().getName())
            .map(Unchecked.function(dbUser -> {
                if (dbUser.isEmpty()) {
                    throw new NotFoundException("Username of your session is not found");
                }

                return new Session(dbUser.get());
            }));
    }

    @POST
    @Path("/logout")
    @RolesAllowed({"User"})
    public Uni<Response> sessionLogout() {
        return Uni
            .createFrom()
            .item(() -> Response
                .ok()
                .cookie(new NewCookie.Builder(cookieName)
                    .value("")
                    .domain(null)
                    .path("/")
                    .expiry(Date.from(Instant.EPOCH))
                    .maxAge(0)
                    .httpOnly(true)
                    .sameSite(NewCookie.SameSite.STRICT)
                    .build())
                .build());
    }
}
