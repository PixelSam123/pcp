package io.github.pixelsam123.pcp.challenge;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ChallengeCreateDto(
    @NotBlank String name,
    @NotNull int tier,
    @NotBlank String description,
    @NotBlank String initialCode,
    @NotBlank String testCase,
    @NotBlank String codeForVerification
) {
}
