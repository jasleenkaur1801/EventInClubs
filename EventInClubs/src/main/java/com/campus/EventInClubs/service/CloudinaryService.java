package com.campus.EventInClubs.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {
    
    private final Cloudinary cloudinary;
    
    public String uploadClubLogo(MultipartFile file) throws IOException {
        try {
            log.info("Starting Cloudinary upload for file: {}, size: {} bytes", 
                file.getOriginalFilename(), file.getSize());
            
            // Upload to Cloudinary
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap(
                    "folder", "club-logos",
                    "resource_type", "image",
                    "transformation", ObjectUtils.asMap(
                        "width", 500,
                        "height", 500,
                        "crop", "limit",
                        "quality", "auto"
                    )
                )
            );
            
            String imageUrl = (String) uploadResult.get("secure_url");
            log.info("Successfully uploaded image to Cloudinary: {}", imageUrl);
            return imageUrl;
            
        } catch (IOException e) {
            log.error("IOException uploading image to Cloudinary: {}", e.getMessage(), e);
            throw new IOException("Failed to upload image to Cloudinary: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error uploading image to Cloudinary: {}", e.getMessage(), e);
            throw new IOException("Unexpected error uploading to Cloudinary: " + e.getMessage(), e);
        }
    }
    
    public void deleteClubLogo(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Successfully deleted image from Cloudinary: {}", publicId);
        } catch (IOException e) {
            log.error("Error deleting image from Cloudinary", e);
        }
    }
    
    // Extract public ID from Cloudinary URL
    public String extractPublicId(String imageUrl) {
        if (imageUrl == null || !imageUrl.contains("cloudinary.com")) {
            return null;
        }
        
        try {
            // URL format: https://res.cloudinary.com/{cloud_name}/image/upload/v{version}/{folder}/{public_id}.{format}
            String[] parts = imageUrl.split("/");
            String lastPart = parts[parts.length - 1];
            String publicIdWithExt = parts[parts.length - 2] + "/" + lastPart;
            
            // Remove file extension
            int dotIndex = publicIdWithExt.lastIndexOf(".");
            if (dotIndex > 0) {
                return publicIdWithExt.substring(0, dotIndex);
            }
            return publicIdWithExt;
        } catch (Exception e) {
            log.error("Error extracting public ID from URL: {}", imageUrl, e);
            return null;
        }
    }
}
