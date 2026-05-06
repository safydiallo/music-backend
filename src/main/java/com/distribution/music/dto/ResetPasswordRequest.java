package com.distribution.music.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {
     @NotBlank
     private String token;
     @Size(min = 6) @NotBlank
     private String newPassword;
}
