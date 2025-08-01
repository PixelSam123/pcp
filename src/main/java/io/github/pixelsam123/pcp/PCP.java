package io.github.pixelsam123.pcp;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

@OpenAPIDefinition(
    info = @Info(
        title = "Pixel Code Platform",
        description = "Self-hostable coding courses/problems platform",
        version = "1.1-SNAPSHOT"
    )
)
public class PCP extends Application {
    @ApplicationScoped
    public Argon2PasswordEncoder argon2PasswordEncoder() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }
}
