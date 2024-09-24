package com.slm.security.controller;

import com.slm.security.model.ApiResponse;
import com.slm.security.utils.JWTUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;

    @RequestMapping("login")
    public ApiResponse<String> login(String username, String password) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(username, password);
        authenticationManager.authenticate(authenticationToken);
        //上一步没有抛出异常说明认证成功，我们向用户颁发jwt令牌
        return ApiResponse.ok(JWTUtil.generateJwtToken(username, password));
    }

}
