package com.slm.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordGenerate {

    public static void main(String[] args) {
        // $2a$10$I2a4XgXFvn0HeV5HiQZuleB3EzxTj3YloTflCTKF98KVepRSAv5tG
        System.out.println(new BCryptPasswordEncoder().encode("123"));
    }

}
