package com.example.Controller;

import com.example.Models.AgendamentoHistorico;
import com.example.Models.Agendamentos;
import com.example.Repository.AgendamentoRepository;
import com.example.Service.AgendamentoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agendamentos")
public class AgendamentoController {

    @Autowired
    private AgendamentoService agendamentoService;

    @GetMapping("/disponibilidade")
    public ResponseEntity <List<LocalTime>> verDisponibilidade(@RequestParam Integer idQuadra,
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data){

        return ResponseEntity.ok(agendamentoService.getHorariosDisponiveis(idQuadra, data));
    }

    @GetMapping("/allAgendamentos")
    public ResponseEntity<List<Agendamentos>> findAllAgendamentos(@RequestParam(required = false) Integer idQuadra,
                                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data){

        List<Agendamentos> lista = agendamentoService.findAllAgendamentos(idQuadra, data);
        return ResponseEntity.ok(lista);
    }

    @PostMapping("/reservar")
    public ResponseEntity<?> criar(@RequestBody Agendamentos agendamento) {
        try {
            Agendamentos novoAgendamento = agendamentoService.createBooking(agendamento);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Agendamento criado com sucesso");
            response.put("agendamento", novoAgendamento);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @GetMapping("/agendamentosClients")
    public ResponseEntity<List<AgendamentoHistorico>> getAgendamentosClients(){
        List<AgendamentoHistorico> lista = agendamentoService.findAgendamentosClients();
        return ResponseEntity.ok(lista);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> atualizarStatus(@PathVariable Integer id, @RequestBody Map<String, String> payload) {
        try {
            String novoStatus = payload.get("status");

            agendamentoService.atualizarStatus(id, novoStatus);
            return ResponseEntity.ok(Map.of("message", "Status atualizado com sucesso!"));

        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/reagendar")
    public ResponseEntity<?> reagendar(@PathVariable Integer id, @RequestBody Map<String, String> payload) {
        try {
            String novaDataStr = payload.get("data_inicio");

            if (novaDataStr == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Data de início é obrigatória."));
            }

            LocalDateTime novaData = LocalDateTime.parse(novaDataStr);

            Agendamentos atualizado = agendamentoService.updateBookingDate(id, novaData);

            return ResponseEntity.ok(Map.of(
                    "message", "Reagendamento realizado com sucesso!",
                    "agendamento", atualizado
            ));

        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Erro ao processar: " + e.getMessage()));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<List<Agendamentos>> getStatusAgendamentos(){
        List<Agendamentos> agendamentos = agendamentoService.findStatusForDashboard();

        return ResponseEntity.ok(agendamentos);
    }

}
