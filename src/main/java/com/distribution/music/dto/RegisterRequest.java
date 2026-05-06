package com.distribution.music.dto;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Le nom complet est obligatoire")
    private String fullName;

    @NotBlank(message = "Le nom d'artiste est obligatoire")
    private String nomArtiste;

    @Email(message = "Format email invalide")
    @NotBlank(message = "L'email est obligatoire")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[@#$%!&*]).{8,}$",
        message = "Le mot de passe doit contenir au moins 8 caractères, une majuscule, un chiffre et un caractère spécial (@#$%!&*)"
    )
    private String password;

    @NotBlank(message = "Le pays est obligatoire")
    private String pays;

    @NotBlank(message = "Le genre musical est obligatoire")
    private String genreMusical;
}