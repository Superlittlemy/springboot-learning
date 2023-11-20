package com.slm.jpa.model;

import org.springframework.beans.factory.annotation.Value;

public interface GenderVO {

    @Value("#{target.name()}")
    String getCode();
    @Value("#{target.getDesc()}")
    String getName();

}
