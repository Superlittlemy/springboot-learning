package com.filterinterceptior.filter;

import org.springframework.stereotype.Component;

import javax.servlet.*;
import java.io.IOException;

/**
 * IP过滤器
 */
@Component
public class SensitiveFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        if ("0:0:0:0:0:0:0:1".equals(request.getRemoteHost()) || "127.0.0.1".equals(request.getRemoteHost())) {
            response.getWriter().append("Not Allowed Ip!");
        } else {
            filterChain.doFilter(request, response);
        }
    }

}
