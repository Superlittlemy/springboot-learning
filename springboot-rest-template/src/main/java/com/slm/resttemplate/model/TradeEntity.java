package com.slm.resttemplate.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeEntity implements Serializable {

    private Long id;
    private String name;
    private LocalDateTime createdDate;
    private Boolean deleted;

}
