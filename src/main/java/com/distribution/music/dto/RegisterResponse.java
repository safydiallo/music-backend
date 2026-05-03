package com.distribution.music.dto;

public record RegisterResponse(
        String message,
        String emailFromName,
        String emailFromAddress,
        String emailTo,
        String emailSubject,
        String emailBody
) {
}
