package com.campus.EventInClubs.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamRegistrationDto {
    private Long id;
    private Long eventId;
    private String eventTitle;
    private String teamName;
    private Integer teamSize;
    private List<String> memberRollNumbers;
    private List<String> memberNames;
    private List<String> memberEmails;
    private Long registeredById;
    private String registeredByName;
    private String registeredByEmail;
    private String status;
    private String registrationNotes;
    private String paymentStatus;
    private LocalDateTime registeredAt;
}
