package com.example.config;

import com.example.Service.RateLimitService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(2)
public class RateLimitFilter implements Filter {

    @Autowired
    private RateLimitService rateLimitService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        String method = req.getMethod();

        if (!"POST".equalsIgnoreCase(method) && !"PUT".equalsIgnoreCase(method) && !"GET".equalsIgnoreCase(method)) {
            chain.doFilter(request, response);
            return;
        }

        String uri = req.getRequestURI();
        String ip = getClientIp(req);
        boolean permitido = verificarLimite(uri, ip);

        if ("GET".equalsIgnoreCase(method)) {
            permitido = rateLimitService.limiteNavegacao(ip);
        } else {
            permitido = verificarLimite(uri, ip);
        }

        
        if (permitido) {
            chain.doFilter(request, response);
        } else {
            res.setStatus(429);
            res.setContentType("application/json");
            res.setCharacterEncoding("UTF-8");
            res.getWriter().write(
                    "{\"success\": false, \"message\": \"Muitas tentativas. Aguarde um momento.\"}"
            );
        }
    }

    private boolean verificarLimite(String uri, String ip) {
        if (uri.contains("/api/auth/login"))          return rateLimitService.Login(ip);
        if (uri.contains("/api/email/enviar-codigo")) return rateLimitService.EnviarEmail(ip);
        if (uri.contains("/api/agendamentos/reservar")) return rateLimitService.Reserva(ip);
        if(uri.contains("/api/users/register-client")) return rateLimitService.RegistrarCliente(ip);
        if(uri.contains("/api/users/register-partner")) return rateLimitService.RegistrarParceiro(ip);
        if (uri.startsWith("/api/agendamentos/") && uri.endsWith("/status")) {
            return rateLimitService.AtualizarStatus(ip);
        }
        return rateLimitService.limiteGlobal(ip);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip != null && !ip.isBlank()) {
            ip = ip.split(",")[0].trim();
        } else {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
