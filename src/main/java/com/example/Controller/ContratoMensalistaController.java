package com.example.Controller;

import com.example.Models.Agendamentos;
import com.example.Models.Arena;
import com.example.Models.ContratoMensalista;
import com.example.Models.Users;
import com.example.Repository.AgendamentoRepository;
import com.example.Repository.ArenaRepository;
import com.example.Repository.ContratoMensalistaRepository;
import com.example.Repository.UserRepository;
import com.example.Service.ContratoMensalistaService;
import com.example.Multitenancy.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contratos-mensalistas")
public class ContratoMensalistaController {

    @Autowired
    private ContratoMensalistaService contratoService;

    @Autowired
    private ContratoMensalistaRepository contratoRepository;

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ArenaRepository arenaRepository;

    /**
     * Helper para obter o utilizador logado através do token JWT.
     * Ajuste conforme a sua implementação exata de segurança.
     */
    private Users getUsuarioLogado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado no sistema"));
    }

    // ==========================================
    // 🟢 ÁREA DO CLIENTE
    // ==========================================

    @PostMapping("/assinar")
    @PreAuthorize("hasAnyAuthority('CLIENTE', 'ADMIN')")
    public ResponseEntity<?> assinarMensalidade(@RequestBody Map<String, Object> payload) {
        try {
            // Extrai os valores do JSON recebido
            Integer idArena = Integer.parseInt(payload.get("idArena").toString());
            Integer idQuadra = Integer.parseInt(payload.get("idQuadra").toString());
            int diaSemana = Integer.parseInt(payload.get("diaSemana").toString());
            String horaInicio = payload.get("horaInicio").toString();
            String horaFim = payload.get("horaFim").toString();

            Users user = getUsuarioLogado();
            Arena arena = arenaRepository.findById(idArena.longValue())
                    .orElseThrow(() -> new RuntimeException("Arena não encontrada"));

            // Força o schema da arena escolhida
            TenantContext.setCurrentTenant(arena.getSchemaName());

            ContratoMensalista contrato = contratoService.criarAssinaturaMensalista(
                    user, arena, idQuadra, diaSemana, LocalTime.parse(horaInicio), LocalTime.parse(horaFim)
            );

            return ResponseEntity.ok(contrato);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } finally {
            TenantContext.clear();
        }
    }

    @GetMapping("/meus-contratos")
    @PreAuthorize("hasAnyAuthority('CLIENTE', 'ADMIN')")
    public ResponseEntity<?> getMeusContratos() {
        try {
            Users user = getUsuarioLogado();

            var contratos = contratoService.listarMeusContratos(user);

            return ResponseEntity.ok(contratos);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Erro ao buscar contratos: " + e.getMessage()));
        }
    }


    // ==========================================
    // 🛠️ ÁREA DO ADMIN (ARENA)
    // ==========================================

    @GetMapping("/arena")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<ContratoMensalista>> getContratosDaArena() {
        // O TenantFilter já tratou do X-Tenant-ID e apontou para o schema correto da arena do Admin.
        List<ContratoMensalista> contratos = contratoRepository.findByAtivoTrue();
        return ResponseEntity.ok(contratos);
    }

    @GetMapping("/arena/{id}/detalhes")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> getDetalhesContratoAdmin(@PathVariable Integer id) {
        try {
            // Busca o contrato no schema da arena
            ContratoMensalista contrato = contratoRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Contrato não encontrado"));

            // Vai ao schema público buscar os dados reais do cliente (Nome, Email)
            TenantContext.clear(); // Limpa temporariamente para ir ao public
            Users cliente = userRepository.findById(contrato.getIdUser()).orElse(null);

            // Devolve as informações mastigadas para o Hover do Admin (Liquid Blur)
            Map<String, Object> detalhes = new HashMap<>();
            detalhes.put("contrato", contrato);
            detalhes.put("clienteNome", cliente != null ? cliente.getNome() : "Desconhecido");
            detalhes.put("clienteEmail", cliente != null ? cliente.getEmail() : "Sem email");
            detalhes.put("clienteTelefone", cliente != null ? cliente.getTelefone() : "Sem telefone");

            return ResponseEntity.ok(detalhes);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Rota de Cancelamento Manual (Se o Admin ou Cliente quiser quebrar o contrato)
    @PutMapping("/cancelar/{id}")
    @PreAuthorize("hasAnyAuthority('CLIENTE', 'ADMIN')")
    public ResponseEntity<?> cancelarContrato(@PathVariable Integer id) {
        try {
            ContratoMensalista contrato = contratoRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Contrato não encontrado"));

            contrato.setAtivo(false);
            contrato.setStatus("CANCELADO");
            contratoRepository.save(contrato);

            return ResponseEntity.ok(Map.of("message", "Contrato cancelado com sucesso. O horário foi libertado para o público."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}