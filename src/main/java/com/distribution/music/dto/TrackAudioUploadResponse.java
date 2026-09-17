package com.distribution.music.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class TrackAudioUploadResponse {
    private TrackResponse track;
    private List<String> warnings;
}
