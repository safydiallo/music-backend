package com.distribution.music.dto;

public record EmailContent(
        String fromName,
        String fromAddress,
        String to,
        String subject,
        String body
) {
}
