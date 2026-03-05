package com.example.Controller;

import com.example.Service.ReportService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/relatorio")
public class ReportController {

    @Autowired
    private ReportService reportService;

    /**
     * Export SuperAdmin report (all arenas + users summary)
     * GET /api/relatorio/superadmin
     */
    @GetMapping("/superadmin")
    public void exportSuperAdminReport(HttpServletResponse response) throws IOException {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"relatorio-superadmin.pdf\"");

        reportService.gerarRelatorioSuperAdmin(response.getOutputStream());
    }

    /**
     * Export Dashboard report (bookings stats + annual billing)
     * GET /api/relatorio/dashboard?ano=2026
     */
    @GetMapping("/dashboard")
    public void exportDashboardReport(
            @RequestParam(defaultValue = "2026") int ano,
            HttpServletResponse response
    ) throws IOException {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"relatorio-dashboard-" + ano + ".pdf\"");

        reportService.gerarRelatorioDashboard(ano, response.getOutputStream());
    }
}
