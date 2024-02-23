package com.slm.ehcache.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConvertDTO {

    private String dicCode;
    private String itemCode;
    private String convertName;

}
