package com.slm.swagger.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "卡")
public class Card {

    private Long id;
    @Schema(title = "卡类型", description = "XXX")
    private String type;
    @Schema(title = "金额")
    private BigDecimal amount;

}
