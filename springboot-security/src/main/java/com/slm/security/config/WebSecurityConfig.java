package com.slm.security.config;

import com.slm.security.config.handler.LoginFailureHandler;
import com.slm.security.config.handler.LoginSuccessHandler;
import com.slm.security.config.handler.MyLogoutSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    private final LoginSuccessHandler loginSuccessHandler;
    private final LoginFailureHandler loginFailureHandler;
    private final MyLogoutSuccessHandler logoutSuccessHandler;

    // 配置密码加密器
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring()
                .antMatchers("/static/**")
                .antMatchers("/index.html")
                .antMatchers("/fronts/**")
                .antMatchers("/*.js")
                .antMatchers("/*.css");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                // 放行接口
                .antMatchers("/common/**").permitAll()
                .antMatchers("/auth/**").permitAll()
                // 除上面外的所有请求全部需要鉴权认证
                .anyRequest().authenticated()
                .and()
                .formLogin()
                // 登录接口
                .loginProcessingUrl("/login")
                // 登录页面
                .loginPage("/login.html")
                // 登录成功处理逻辑
                .successHandler(loginSuccessHandler)
                // 默认成功页面，第三参数如果为true，登录成功会固定调转该页面
                .defaultSuccessUrl("/index.html", true)
                // 登录失败处理逻辑
                .failureHandler(loginFailureHandler)
                .permitAll()
                .and()
                .logout()
                // 注销接口
                .logoutUrl("/logout")
                .logoutSuccessHandler(logoutSuccessHandler)
                // 注销后删除 cookies
                .deleteCookies("JSESSIONID")
                .permitAll()
                // 关闭 csrf 防御
                .and().csrf().disable();
    }
}