package com.campus.EventInClubs.controller;

import com.campus.EventInClubs.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@Slf4j
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class FileUploadController {
    
    private final CloudinaryService cloudinaryService;
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final String[] ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".gif", ".webp"};
    
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
            
            // Upload to Cloudinary
            String imageUrl = cloudinaryService.uploadClubLogo(file);
            log.info("Uploaded club logo to Cloudinary: {}", imageUrl);
            
            // Return the Cloudinary URL
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "fileUrl", imageUrl,
                    "filename", originalFilename
            ));
            
        } catch (IOException e) {
            log.error("Error uploading file", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to upload file: " + e.getMessage()));
        }
    }
    
    @DeleteMapping("/delete-club-logo")
    public ResponseEntity<?> deleteClubLogo(@RequestParam String imageUrl) {
        try {
            String publicId = cloudinaryService.extractPublicId(imageUrl);
            if (publicId != null) {
                cloudinaryService.deleteClubLogo(publicId);
                log.info("Deleted club logo from Cloudinary: {}", publicId);
                return ResponseEntity.ok(Map.of("success", true));
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid image URL"));
            }
        } catch (Exception e) {
            log.error("Error deleting file", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to delete file"));
        }
    }
}
