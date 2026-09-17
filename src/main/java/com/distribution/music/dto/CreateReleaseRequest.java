package com.distribution.music.dto;

import com.distribution.music.entity.ReleaseType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateReleaseRequest {
    @NotNull(message = "Le type de sortie est requis (SINGLE, EP ou ALBUM)")
    private ReleaseType type;

    private String title;
}
