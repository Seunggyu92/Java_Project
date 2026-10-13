package com.volunteerintegration.member;

import java.time.LocalDateTime;

public record Member(
        long id,
        String email,
        String pwHash,
        String name,
        String phone,
        String bloodType,
        String sido,
        LocalDateTime createdAt) {
}
