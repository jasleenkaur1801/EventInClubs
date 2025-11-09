package com.campus.EventInClubs.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudinaryConfig {
    
    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(ObjectUtils.asMap(
            "cloud_name", "dbzrxx75s",
            "api_key", "245416923686528",
            "api_secret", "Kp6R8xFSOtI7dyY4KScCDJCIO5A",
            "secure", true
        ));
    }
}
