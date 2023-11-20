package com.slm.jpa.model;


import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public interface UserVO {

    String getName();
    GenderVO getGender();
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime getCreatedDate();

}
