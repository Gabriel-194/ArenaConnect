package com.example.Multitenancy;

import jakarta.servlet.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.io.IOException;
import com.example.Service.JwtService;


@Component
@Order(1)
@DependsOn("entityManagerFactory")
public class TenantFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(TenantFilter.class);

    @Autowired
    private JwtService jwtService;


    private boolean isPublicEndpoint(String uri) {

        if (uri.equals("/") || uri.equals("/login") || uri.equals("/register")) {
            return true;
        }

        if (uri.endsWith(".css") || uri.endsWith(".js") || uri.endsWith(".png") || uri.endsWith(".svg") || uri.endsWith(".jpg") || uri.endsWith(".ico")) {
            return true;
        }

        if (uri.startsWith("/api/users/register")) {
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

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;


        String requestURI = req.getRequestURI();
        if (isPublicEndpoint(requestURI)) {
            chain.doFilter(request, response);
            return;
        }

        String token = null;
        if (req.getCookies() != null) {
            for (Cookie cookie : req.getCookies()) {
                if ("accessToken".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        String tenantHeader = req.getHeader("X-Tenant-ID");

        if (tenantHeader != null && !tenantHeader.isEmpty()) {

            TenantContext.setCurrentTenant(tenantHeader);
        } else if (token != null) {
            String tenantSchema = jwtService.getArenaSchemaFromToken(token);
            if (tenantSchema != null) {
                TenantContext.setCurrentTenant(tenantSchema);
            }
        }
        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}