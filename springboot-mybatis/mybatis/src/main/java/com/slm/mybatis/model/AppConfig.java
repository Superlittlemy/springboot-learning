package com.slm.mybatis.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppConfig {

    private Boolean hasUpdate;
    private String url;

}
