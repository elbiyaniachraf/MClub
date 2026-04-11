package io.droidevs.mclub.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class MembershipDto {
    private UUID id;
    private UUID userId;
    private UUID clubId;
    private String role;
    private String status;
    private LocalDateTime joinedAt;

    // UI helper fields
    private String userFullName;
    private String userEmail;
}
