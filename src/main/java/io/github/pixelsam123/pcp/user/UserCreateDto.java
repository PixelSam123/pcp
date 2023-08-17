package io.github.pixelsam123.pcp.user;

import jakarta.validation.constraints.NotBlank;

public record UserCreateDto(
    @NotBlank String name,
    @NotBlank String password
) {
}
