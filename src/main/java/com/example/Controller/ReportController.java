package com.example.Controller;

import com.example.Service.ReportService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/relatorio")
public class ReportController {

    @Autowired
    private ReportService reportService;


    @GetMapping("/superadmin")
    public void exportSuperAdminReport(HttpServletResponse response) throws IOException {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"relatorio-superadmin.pdf\"");

        reportService.gerarRelatorioSuperAdmin(response.getOutputStream());
    }


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

    @GetMapping("/agendamentos")
    public void exportAgendamentosReport(
            @RequestParam(required = false) Integer idQuadra,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(required = false) String status,
            HttpServletResponse response
    ) throws IOException {
        String filePart = data != null ? "-" + data : "";
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"relatorio-agendamentos" + filePart + ".pdf\"");
        reportService.gerarRelatorioAgendamentos(idQuadra, data, status, response.getOutputStream());
    }
}
