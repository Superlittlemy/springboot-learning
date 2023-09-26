package com.slm.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class Order {

    private int id;
    private String buyer;
    private LocalDateTime createdDate;

}
