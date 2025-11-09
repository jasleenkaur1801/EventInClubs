package com.campus.EventInClubs.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class CloudinaryConfig {
    
    @Value("${cloudinary.cloud-name:dbzrxx75s}")
    private String cloudName;
    
    @Value("${cloudinary.api-key:245416923686528}")
    private String apiKey;
    
    @Value("${cloudinary.api-secret:Kp6R8xFSOtI7dyY4KScCDJCIO5A}")
    private String apiSecret;
    
    @Bean
    public Cloudinary cloudinary() {
        try {
            log.info("Initializing Cloudinary with cloud_name: {}", cloudName);
            log.info("API Key length: {}", apiKey != null ? apiKey.length() : "null");
            log.info("API Secret length: {}", apiSecret != null ? apiSecret.length() : "null");
            
            Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
            ));
            
            log.info("Cloudinary initialized successfully");
            return cloudinary;
        } catch (Exception e) {
            log.error("Failed to initialize Cloudinary: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize Cloudinary", e);
        }
    }
}
