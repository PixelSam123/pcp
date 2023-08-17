package io.github.pixelsam123.pcp;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

@OpenAPIDefinition(
    info = @Info(
        title = "Pixel Code Platform",
        description = "Self-hostable coding courses/problems platform",
        version = "1.0-SNAPSHOT"
    ),
    tags = {
        @Tag(name = "users", description = "User creation, viewing and editing"),
        @Tag(name = "challenges", description = "Challenge creation, viewing and editing"),
        @Tag(
            name = "challenge_comments",
            description = "Challenge comment creation, viewing and editing"
        ),
        @Tag(
            name = "challenge_votes", description = "Challenge vote creation, viewing and editing"
        ),
        @Tag(
            name = "challenge_submissions",
            description = "Challenge submission creation, viewing and editing"
        ),
        @Tag(
            name = "challenge_submission_comments",
            description = "Challenge submission comment creation, viewing and editing"
        ),
        @Tag(
            name = "challenge_submission_votes",
            description = "Challenge submission vote creation, viewing and editing"
        )
    }
)
public class PCP {
    @ApplicationScoped
    public Argon2PasswordEncoder argon2PasswordEncoder() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }
}
