package com.campus.EventInClubs.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClubDto {
    
    private Long id;
    private String name;
    private String description;
    private String category;
    private String shortName;
    private Integer memberCount;
    private Integer eventCount;
    private Double rating;
    private String logoUrl;
    private Long adminUserId;
    private String adminUserName;
    private Boolean isActive;
    private String approvalStatus;
    private Instant createdAt;
    private Instant updatedAt;
}
