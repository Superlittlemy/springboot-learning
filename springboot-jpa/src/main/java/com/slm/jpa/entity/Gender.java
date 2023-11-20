package com.slm.jpa.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Gender {

    MALE("男性"), FEMALE("女性");

    private final String desc;

}
