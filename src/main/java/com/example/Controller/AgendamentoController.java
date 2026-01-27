package com.example.Controller;

import com.example.Models.Agendamentos;
import com.example.Repository.AgendamentoRepository;
import com.example.Service.AgendamentoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agendamentos")
@PreAuthorize("hasAnyRole('CLIENTE', 'ADMIN')")
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
            error.put("message", "Erro interno ao criar agendamento");
            return ResponseEntity.internalServerError().body(error);
        }
    }
}
