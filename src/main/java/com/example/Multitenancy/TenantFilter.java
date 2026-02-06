package com.example.Multitenancy;

import com.example.Models.Arena;
import com.example.Repository.ArenaRepository;
import com.example.Service.ArenaService;
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
import java.util.Optional;

import com.example.Service.JwtService;


@Component
@Order(1)
@DependsOn("entityManagerFactory")
public class TenantFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(TenantFilter.class);

    @Autowired
    private JwtService jwtService;
    @Autowired
    private ArenaRepository arenaRepository;


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

        if (isPublicEndpoint(req.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        String tenantSchema = resolveTenant(req);

        if (tenantSchema != null && !"public".equals(tenantSchema)) {

            Optional<Arena> arenaOpt = arenaRepository.findBySchemaName(tenantSchema);

            if (arenaOpt.isPresent() && !arenaOpt.get().isAtivo()) {
                logger.warn("🚫 BLOQUEIO: Arena inativa: {}", arenaOpt.get().getName());

                res.setStatus(402);
                res.setContentType("application/json");
                res.setCharacterEncoding("UTF-8");
                res.getWriter().write(
                        "{\"message\":\"Arena bloqueada. Pagamento pendente.\",\"forceModal\":true}"
                );
                return;
            }

            TenantContext.setCurrentTenant(tenantSchema);
        }

        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private String resolveTenant(HttpServletRequest req) {

        String tenantHeader = req.getHeader("X-Tenant-ID");
        if (tenantHeader != null && !tenantHeader.isBlank()) {
            return tenantHeader;
        }

        if (req.getCookies() != null) {
            for (Cookie cookie : req.getCookies()) {
                if ("accessToken".equals(cookie.getName())) {
                    return jwtService.getArenaSchemaFromToken(cookie.getValue());
                }
            }
        }

        return null;
    }
}