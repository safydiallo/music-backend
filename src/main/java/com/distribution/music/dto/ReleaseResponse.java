package com.distribution.music.dto;

import com.distribution.music.entity.ReleaseStatus;
import com.distribution.music.entity.ReleaseType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ReleaseResponse {
    private Long id;
    private String title;
    private ReleaseType type;
    private ReleaseStatus status;
    private String coverUrl;
    private int trackCount;
    private LocalDateTime updatedAt;
}
