package com.campus.EventInClubs.config;

import com.campus.EventInClubs.domain.model.Hall;
import com.campus.EventInClubs.domain.model.Role;
import com.campus.EventInClubs.domain.model.User;
import com.campus.EventInClubs.repository.HallRepository;
import com.campus.EventInClubs.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner seedSuperAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            String email = "superadmin@eventinclubs.com";
            String rawPassword = "SuperAdmin@2025";

            userRepository.findByEmail(email).ifPresentOrElse(
                    existing -> {},
                    () -> {
                        User superAdmin = User.builder()
                                .name("Super Admin")
                                .email(email)
                                .passwordHash(passwordEncoder.encode(rawPassword))
                                .role(Role.SUPER_ADMIN)
                                .createdAt(Instant.now())
                                .updatedAt(Instant.now())
                                .build();
                        userRepository.save(superAdmin);
                    }
            );
        };
    }

    @Bean
    public CommandLineRunner seedHalls(HallRepository hallRepository) {
        return args -> {
            // Check if halls already exist
            if (hallRepository.count() == 0) {
                // Create sample halls
                Hall[] halls = {
                    Hall.builder()
                        .name("Main Auditorium")
                        .seatingCapacity(500)
                        .description("Large auditorium with stage and sound system")
                        .location("Ground Floor, Main Building")
                        .facilities("Projector,Sound System,AC,Stage,Microphones")
                        .isActive(true)
                        .build(),
                    
                    Hall.builder()
                        .name("Conference Hall A")
                        .seatingCapacity(100)
                        .description("Medium-sized conference hall for seminars")
                        .location("First Floor, Academic Block")
                        .facilities("Projector,AC,Whiteboard,WiFi")
                        .isActive(true)
                        .build(),
                    
                    Hall.builder()
                        .name("Conference Hall B")
                        .seatingCapacity(80)
                        .description("Smaller conference hall for workshops")
                        .location("First Floor, Academic Block")
                        .facilities("Projector,AC,Whiteboard")
                        .isActive(true)
                        .build(),
                    
                    Hall.builder()
                        .name("Seminar Room 1")
                        .seatingCapacity(50)
                        .description("Intimate seminar room for small gatherings")
                        .location("Second Floor, Academic Block")
                        .facilities("TV Display,AC,Whiteboard")
                        .isActive(true)
                        .build(),
                    
                    Hall.builder()
                        .name("Seminar Room 2")
                        .seatingCapacity(40)
                        .description("Small seminar room for team meetings")
                        .location("Second Floor, Academic Block")
                        .facilities("TV Display,AC")
                        .isActive(true)
                        .build(),
                    
                    Hall.builder()
                        .name("Multipurpose Hall")
                        .seatingCapacity(200)
                        .description("Flexible space for various events")
                        .location("Ground Floor, Student Center")
                        .facilities("Sound System,AC,Movable Chairs")
                        .isActive(true)
                        .build()
                };
                
                for (Hall hall : halls) {
                    hallRepository.save(hall);
                }
                
                System.out.println("Initialized " + halls.length + " sample halls");
            }
        };
    }
}



