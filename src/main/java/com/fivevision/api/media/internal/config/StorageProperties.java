package com.fivevision.api.media.internal.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "storage")
@Getter
@Setter
public class StorageProperties {
    private String endpointUrl;
    private String accessKey;
    private String secretKey;
    private String region;
    private String bucketName;
}