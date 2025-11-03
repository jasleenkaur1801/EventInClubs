package com.campus.EventInClubs.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
@Slf4j
@CrossOrigin(origins = "*")
public class FileUploadController {
    
    private static final String UPLOAD_DIR = "uploads/club-logos/";
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final String[] ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".gif", ".webp"};
    
    public FileUploadController() {
        // Create upload directory if it doesn't exist
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("Created upload directory: {}", UPLOAD_DIR);
            }
        } catch (IOException e) {
            log.error("Could not create upload directory", e);
        }
    }
    
    @PostMapping("/upload-club-logo")
    public ResponseEntity<?> uploadClubLogo(@RequestParam("file") MultipartFile file) {
        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Please select a file to upload"));
            }
            
            // Check file size
            if (file.getSize() > MAX_FILE_SIZE) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "File size exceeds maximum limit of 5MB"));
            }
            
            // Check file extension
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid file"));
            }
            
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            boolean isValidExtension = false;
            for (String ext : ALLOWED_EXTENSIONS) {
                if (ext.equals(fileExtension)) {
                    isValidExtension = true;
                    break;
                }
            }
            
            if (!isValidExtension) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Only image files (jpg, jpeg, png, gif, webp) are allowed"));
            }
            
            // Generate unique filename
            String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
            Path filePath = Paths.get(UPLOAD_DIR + uniqueFilename);
            
            // Save file
            Files.copy(file.getInputStream(), filePath);
            log.info("Uploaded club logo: {}", uniqueFilename);
            
            // Return the file URL (relative path)
            String fileUrl = "/" + UPLOAD_DIR + uniqueFilename;
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "fileUrl", fileUrl,
                    "filename", uniqueFilename
            ));
            
        } catch (IOException e) {
            log.error("Error uploading file", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to upload file"));
        }
    }
    
    @DeleteMapping("/delete-club-logo")
    public ResponseEntity<?> deleteClubLogo(@RequestParam String filename) {
        try {
            Path filePath = Paths.get(UPLOAD_DIR + filename);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Deleted club logo: {}", filename);
                return ResponseEntity.ok(Map.of("success", true));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IOException e) {
            log.error("Error deleting file", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to delete file"));
        }
    }
}
