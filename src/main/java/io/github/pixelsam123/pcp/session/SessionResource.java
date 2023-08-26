package io.github.pixelsam123.pcp.session;

import io.github.pixelsam123.pcp.user.UserRepository;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import java.time.Instant;
import java.util.Date;

@Path("/session")
public class SessionResource {
    private final UserRepository userRepository;

    public SessionResource(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GET
    @RolesAllowed({"User"})
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Session> sessionGet(@Context SecurityContext ctx) {
        return userRepository
            .asyncFindByNameBrief(ctx.getUserPrincipal().getName())
            .map(Unchecked.function(dbUser -> {
                if (dbUser.isEmpty()) {
                    throw new BadRequestException(Response
                        .status(Response.Status.BAD_REQUEST)
                        .header("WWW-Authenticate", "Bearer")
                        .entity("Incorrect username")
                        .build());
                }

                return new Session(dbUser.get());
            }));
    }

    @POST
    @Path("/logout")
    @RolesAllowed({"User"})
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> sessionLogout() {
        return Uni
            .createFrom()
            .item(
                () -> Response
                    .ok()
                    .cookie(
                        new NewCookie.Builder("quarkus-credential")
                            .value("")
                            .path("/")
                            .domain(null)
                            .maxAge(0)
                            .expiry(Date.from(Instant.EPOCH))
                            .build()
                    )
                    .build()
            );
    }
}
