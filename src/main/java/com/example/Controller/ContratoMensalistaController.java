package com.example.Controller;

import com.example.Models.Agendamentos;
import com.example.Models.Arena;
import com.example.Models.ContratoMensalista;
import com.example.Models.Users;
import com.example.Repository.AgendamentoRepository;
import com.example.Repository.ArenaRepository;
import com.example.Repository.ContratoMensalistaRepository;
import com.example.Repository.UserRepository;
import com.example.Service.AgendamentoService;
import com.example.Service.AsaasService;
import com.example.Service.ContratoMensalistaService;
import com.example.Multitenancy.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contratos-mensalistas")
public class ContratoMensalistaController {
    @Autowired
    private AsaasService asaasService;

    @Autowired
    private ContratoMensalistaService contratoService;

    @Autowired
    private ContratoMensalistaRepository contratoRepository;

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    @Autowired
    private AgendamentoService agendamentoService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ArenaRepository arenaRepository;

    private Users getUsuarioLogado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado no sistema"));
    }

    // ==========================================
    // 🟢 ÁREA DO CLIENTE
    // ==========================================

    @PostMapping("/assinar")
    @PreAuthorize("hasAnyRole('CLIENTE', 'ADMIN')")
    public ResponseEntity<?> assinarMensalidade(@RequestBody Map<String, Object> payload) {
        try {
            // Lemos os dados a partir do JSON (Body) que o React envia
            Integer idArena = Integer.parseInt(payload.get("idArena").toString());
            Integer idQuadra = Integer.parseInt(payload.get("idQuadra").toString());
            int diaSemana = Integer.parseInt(payload.get("diaSemana").toString());
            LocalDate dataInicio = LocalDate.parse(payload.get("dataInicio").toString());
            String horaInicio = payload.get("horaInicio").toString();
            String horaFim = payload.get("horaFim").toString();

            Users user = getUsuarioLogado();
            Arena arena = arenaRepository.findById(idArena.longValue())
                    .orElseThrow(() -> new RuntimeException("Arena não encontrada"));

            // Força o schema da arena escolhida para guardar o contrato no lugar certo
            TenantContext.setCurrentTenant(arena.getSchemaName());

            ContratoMensalista contrato = contratoService.criarAssinaturaMensalista(
                    user, arena, idQuadra, diaSemana, dataInicio, LocalTime.parse(horaInicio), LocalTime.parse(horaFim)
            );

            return ResponseEntity.ok(contrato);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } finally {
            TenantContext.clear();
        }
    }

    @GetMapping("/meus-contratos")
    @PreAuthorize("hasAnyRole('CLIENTE', 'ADMIN')")
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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ContratoMensalista>> getContratosDaArena() {
        List<ContratoMensalista> contratos = contratoRepository.findByAtivoTrue();
        return ResponseEntity.ok(contratos);
    }

    @GetMapping("/arena/{id}/detalhes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getDetalhesContratoAdmin(@PathVariable Integer id) {
        try {
            ContratoMensalista contrato = contratoRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Contrato não encontrado"));

            TenantContext.clear();
            Users cliente = userRepository.findById(contrato.getIdUser()).orElse(null);

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

    @PutMapping("/cancelar/{id}")
    public ResponseEntity<?> cancelarContrato(@PathVariable Integer id) {
        try {
            List<Arena> arenas = arenaRepository.findAll();
            ContratoMensalista contratoEncontrado = null;

            // 1. Busca Inteligente: Procura em qual arena este contrato existe
            for (Arena arena : arenas) {
                if (arena.getSchemaName() == null || "public".equals(arena.getSchemaName())) continue;

                TenantContext.setCurrentTenant(arena.getSchemaName());
                java.util.Optional<ContratoMensalista> opt = contratoRepository.findById(id);

                if (opt.isPresent()) {
                    contratoEncontrado = opt.get();
                    break; // Encontrou a arena correta!
                }
                TenantContext.clear();
            }

            if (contratoEncontrado == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Contrato não encontrado no sistema."));
            }

            // 2. Regra de Negócio: Só cancela se não estiver pago (PENDENTE)
            if (!"PENDENTE".equals(contratoEncontrado.getStatus())) {
                TenantContext.clear();
                return ResponseEntity.badRequest().body(Map.of("error", "Apenas mensalidades pendentes podem ser canceladas."));
            }

            // 3. Cancelamento no Asaas Gateway
            if (contratoEncontrado.getAsaasPaymentId() != null) {
                try {
                    asaasService.cancelarCobranca(contratoEncontrado.getAsaasPaymentId());
                } catch (Exception e) {
                    System.err.println("Aviso: Falha ao cancelar cobrança do contrato no Asaas: " + e.getMessage());
                }
            }

            // 4. Cancelar o Contrato no Banco de Dados
            contratoEncontrado.setAtivo(false);
            contratoEncontrado.setStatus("CANCELADO");
            contratoRepository.save(contratoEncontrado);

            if (contratoEncontrado.getAsaasPaymentId() != null) {
                agendamentoService.cancelPaymentWebhook(contratoEncontrado.getAsaasPaymentId(), "CANCELAMENTO_MANUAL");
            }

            // 5. Cascata: Cancelar todos os agendamentos (jogos) futuros ligados a este contrato
            List<Agendamentos> todosAgendamentos = agendamentoRepository.findAll();
            for (Agendamentos ag : todosAgendamentos) {
                // Filtra os jogos que são desta quadra, deste usuário e que são "MENSALISTAS"
                if (ag.getId_user().equals(contratoEncontrado.getIdUser()) &&
                        ag.getId_quadra().equals(contratoEncontrado.getIdQuadra()) &&
                        ag.getStatus() != null && ag.getStatus().startsWith("MENSALISTA_") &&
                        !ag.getStatus().equals("CANCELADO") && !ag.getStatus().equals("FINALIZADO")) {

                    ag.setStatus("CANCELADO");
                    agendamentoRepository.save(ag);

                    // Se o jogo gerou uma fatura separada no Asaas, cancela também
                    if (ag.getAsaasPaymentId() != null && !ag.getAsaasPaymentId().equals(contratoEncontrado.getAsaasPaymentId())) {
                        try { asaasService.cancelarCobranca(ag.getAsaasPaymentId()); }
                        catch (Exception ignored) {}
                    }
                }
            }

            return ResponseEntity.ok(Map.of("message", "Contrato e jogos vinculados cancelados com sucesso."));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } finally {
            TenantContext.clear();
        }
    }
}
