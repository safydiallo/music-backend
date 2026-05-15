package com.distribution.music.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ChangePasswordRequest {

    @NotBlank(message = "Le nouveau mot de passe est obligatoire")
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[@#$%!&*]).{8,}$",
        message = "Le mot de passe doit contenir au moins 8 caractères, une majuscule, un chiffre et un caractère spécial"
    )
    private String newPassword;

    @NotBlank(message = "La confirmation est obligatoire")
    private String confirmPassword;
}
