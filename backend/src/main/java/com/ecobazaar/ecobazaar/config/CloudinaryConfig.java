package com.ecobazaar.ecobazaar.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudinaryConfig {

    @Bean
    public Cloudinary cloudinary() {
        // Use env vars in production; falls back to empty strings locally
        // (image upload will fail gracefully without real credentials)
        String cloudName = "dkuiluszr";
        String apiKey = "893825318738397";
        String apiSecret = "CKsCDkC9Pse0fXBsOuwrIoWYxSc";

        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret));
    }

    private String getEnv(String name, String defaultValue) {
        String val = System.getenv(name);
        return (val != null && !val.isBlank()) ? val : defaultValue;
    }
}
