package com.example.Multitenancy;

import jakarta.servlet.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import java.io.IOException;


@Component
@Order(1)
@DependsOn("entityManagerFactory")
public class TenantFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(TenantFilter.class);


    private boolean isPublicEndpoint(String uri) {

        if (uri.equals("/") || uri.equals("/login") || uri.equals("/register")) {
            return true;
        }

        if (uri.endsWith(".css") || uri.endsWith(".js") || uri.endsWith(".png") || uri.endsWith(".svg") || uri.endsWith(".jpg") || uri.endsWith(".ico")) {
            return true;
        }

        return uri.startsWith("/api/auth/") ||
                uri.startsWith("/api/users/register") ||
                uri.startsWith("/styles/") ||
                uri.startsWith("/scripts/") ||
                uri.startsWith("/assets/") ||
                uri.startsWith("/images/");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        chain.doFilter(request, response);
    }
}