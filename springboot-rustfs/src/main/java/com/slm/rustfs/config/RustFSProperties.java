package com.slm.rustfs.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "rustfs")
public class RustFSProperties {

    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucket;
    private Integer presignExpirationSeconds;

}
