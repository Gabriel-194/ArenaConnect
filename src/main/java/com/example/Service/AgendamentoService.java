package com.example.Service;

import com.example.DTOs.Asaas.AsaasResponseDTO;
import com.example.Domain.RoleEnum;
import com.example.Models.*;
import com.example.Multitenancy.TenantContext;
import com.example.Repository.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AgendamentoService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QuadraRepository quadraRepository;

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    @Autowired
    private ArenaRepository arenaRepository;

    @Autowired
    private HistoricoRepository historicoRepository;

    @Autowired
    private AsaasService asaasService;


    private String configurarSchema() {
        String currentTenant = TenantContext.getCurrentTenant();

        if (currentTenant != null && !currentTenant.isEmpty()) {
            return currentTenant;
        } else {
            return "public";
        }
    }

    @Transactional
    public List<LocalTime> getHorariosDisponiveis(Integer idQuadra, LocalDate data) {
        String schema = configurarSchema();

        Arena arena = arenaRepository.findBySchemaName(schema)
                .orElseThrow(() -> new RuntimeException("Arena não encontrada para o schema: " + schema));

        LocalTime abertura = (arena.getHoraInicio() != null) ? arena.getHoraInicio() : LocalTime.of(6, 00);
        LocalTime fechamento = (arena.getHoraFim() != null) ? arena.getHoraFim() : LocalTime.of(23, 00);

        LocalDateTime inicioDia = data.atStartOfDay();
        LocalDateTime fimDia = data.plusDays(1).atStartOfDay();

        List<Agendamentos> agendamentos = agendamentoRepository.findAgendamentosDoDiaComSchema(idQuadra, inicioDia, fimDia, schema);

        List<LocalTime> horariosOcupados = agendamentos.stream()
                .map(a -> a.getData_inicio().toLocalTime())
                .collect(Collectors.toList());

        List<LocalTime> horariosDisponiveis = new ArrayList<>();
        LocalTime atual = abertura;

        while (atual.isBefore(fechamento)) {
            if (!horariosOcupados.contains(atual)) {
                horariosDisponiveis.add(atual);
            }
            atual = atual.plusHours(1);
        }
        return horariosDisponiveis;
    }

    @Transactional
    public Agendamentos createBooking(Agendamentos newBooking) {
        String emailUsuarioLogado = SecurityContextHolder.getContext().getAuthentication().getName();
        Users currentUser = userRepository.findByEmail(emailUsuarioLogado)
                .orElseThrow(() -> new RuntimeException("Usuário logado não encontrado."));

        String currentSchema = configurarSchema();

        if (newBooking.getId_agendamento() == null) {
            newBooking.setId_user(currentUser.getIdUser());
            newBooking.setStatus("PENDENTE");
        }

        if (newBooking.getId_quadra() == null || newBooking.getData_inicio() == null) {
            throw new IllegalArgumentException("Quadra e Data de início são obrigatórias");
        }

        newBooking.setData_fim(newBooking.getData_inicio().plusHours(1));

        List<LocalTime> disponiveis = getHorariosDisponiveis(newBooking.getId_quadra(), newBooking.getData_inicio().toLocalDate());
        if (!disponiveis.contains(newBooking.getData_inicio().toLocalTime())) {
            throw new IllegalArgumentException("Horário indisponível!");
        }

        Arena arena = arenaRepository.findBySchemaName(currentSchema)
                .orElseThrow(() -> new RuntimeException("Arena não encontrada"));

        if (arena.getAsaasWalletId() == null) {
            throw new RuntimeException("Esta Arena ainda não está configurada para receber pagamentos.");
        }


        if (currentUser.getAsaasCustomerId() == null) {
            String newCustomerId = asaasService.createCustomer(currentUser);
            currentUser.setAsaasCustomerId(newCustomerId);
            userRepository.save(currentUser);
        }

        try {
            AsaasResponseDTO cobranca = asaasService.createPaymentWithSplit(
                    currentUser.getAsaasCustomerId(),
                    newBooking.getValor(),
                    arena.getAsaasWalletId()
            );

            newBooking.setAsaasPaymentId(cobranca.getId());
            newBooking.setAsaasInvoiceUrl(cobranca.getInvoiceUrl());

            if (cobranca.getPix() != null) {
                newBooking.setPixQrCode(cobranca.getPix().getEncodedImage());
                newBooking.setPixCopyPaste(cobranca.getPix().getPayload());
            }

        } catch (Exception e) {
            throw new RuntimeException("Falha ao gerar pagamento: " + e.getMessage());
        }

        Agendamentos savedBooking = agendamentoRepository.salvarComSchema(newBooking, currentSchema);

        salvarHistorico(savedBooking, currentSchema, arena);

        return savedBooking;
    }

    private void salvarHistorico(Agendamentos original, String schema, Arena arena) {
        try {
            AgendamentoHistorico historico = new AgendamentoHistorico();
            historico.setSchemaName(schema);
            historico.setIdUser(original.getId_user());
            historico.setIdAgendamento(original.getId_agendamento());
            historico.setId_quadra(original.getId_quadra());
            historico.setDataInicio(original.getData_inicio());
            historico.setData_fim(original.getData_fim());
            historico.setStatus(original.getStatus());
            historico.setValor(original.getValor());
            historico.setAsaasPaymentId(original.getAsaasPaymentId());
            historico.setAsaasInvoiceUrl(original.getAsaasInvoiceUrl());

            historico.setArenaName(arena.getName());
            historico.setEnderecoArena(arena.getEndereco() + " - " + arena.getCidade());

            quadraRepository.buscarPorIdComSchema(original.getId_quadra(), schema).ifPresent(quadra -> {
                historico.setQuadraNome(quadra.getNome());
            });

            historicoRepository.save(historico);
        } catch (Exception e) {
            System.err.println("Erro ao salvar histórico: " + e.getMessage());
        }
    }

    @Transactional
    public List<Agendamentos> findAllAgendamentos(Integer idQuadra, LocalDate data) {
        String schema = configurarSchema();

        if (schema == null || schema.isEmpty()) {
            throw new IllegalArgumentException("O identificador da arena (schema) é obrigatório.");
        }
        List<Agendamentos> agendamento = agendamentoRepository.findAllAgendamentos(idQuadra, data, schema);

        for (Agendamentos a : agendamento) {
            String nomeCliente = userRepository.findById(a.getId_user())
                    .map(user -> user.getNome())
                    .orElse("Usuario não encontrado");

            a.setNomeCliente(nomeCliente);

            String nomeQuadra = quadraRepository.findById(a.getId_quadra())
                    .map(quadra -> quadra.getNome())
                    .orElse("Quadra não encontrada");

            a.setQuadraNome(nomeQuadra);

        }

        return new ArrayList<>(agendamento);
    }

    @Transactional
    public List<AgendamentoHistorico> findAgendamentosClients() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        List<AgendamentoHistorico> historico = historicoRepository.buscarHistoricoPorUsuario(user.getIdUser());


        for (AgendamentoHistorico h : historico) {
            if (h.getAsaasPaymentId() != null && h.getAsaasInvoiceUrl() == null) {

                String urlRecuperada = asaasService.getPaymentUrlById(h.getAsaasPaymentId());

                if (urlRecuperada != null) {
                    h.setAsaasInvoiceUrl(urlRecuperada);
                    historicoRepository.save(h);
                }
            }
        }

        return historico;
    }

    @Transactional
    public void atualizarStatus(Integer idAgendamento, String novoStatus) {
        String schema = configurarSchema();
        String statusAlvo = novoStatus.toUpperCase();

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Users usuarioLogado = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não autenticado."));

        boolean isAdmin = usuarioLogado.getRole() == RoleEnum.ADMIN || usuarioLogado.getRole() == RoleEnum.SUPERADMIN;

        Agendamentos agendamento = agendamentoRepository.buscarPorIdComSchema(idAgendamento, schema)
                .orElseThrow(() -> new RuntimeException("Agendamento não encontrado."));


        if (!isAdmin) {
            if (!agendamento.getId_user().equals(usuarioLogado.getIdUser())) {
                throw new SecurityException("Você não tem permissão para alterar este agendamento.");
            }

            if (statusAlvo.equals("FINALIZADO") || statusAlvo.equals("CONFIRMADO")) {
                throw new SecurityException("Ação não permitida. Aguarde a confirmação do pagamento.");
            }

            if (!statusAlvo.equals("CANCELADO")) {
                throw new SecurityException("Status inválido para operação de cliente.");
            }

        }

        if (statusAlvo.equals("CANCELADO")) {
            if (agendamento.getAsaasPaymentId() != null) {
                try {
                    asaasService.cancelarCobranca(agendamento.getAsaasPaymentId());

                    agendamento.setAsaasInvoiceUrl(null);
                } catch (Exception e) {
                    System.err.println("Erro ao remover cobrança Asaas: " + e.getMessage());
                }
            }
        }

        agendamento.setStatus(statusAlvo);
        agendamentoRepository.salvarComSchema(agendamento, schema);

        historicoRepository.buscarPorOrigem(idAgendamento, schema).ifPresent(hist -> {
            hist.setStatus(statusAlvo);
            if (statusAlvo.equals("CANCELADO")) {
                hist.setAsaasInvoiceUrl(null);
            }
            historicoRepository.save(hist);
        });
    }

    public boolean confirmPaymentWebhook(String paymentId){
        var historicoOpt = historicoRepository.findByAsaasPaymentId(paymentId);

        if(historicoOpt.isPresent()){
            AgendamentoHistorico historico = historicoOpt.get();
            String targetSchema = historico.getSchemaName();

            TenantContext.setCurrentTenant(targetSchema);

            try{
                agendamentoRepository.findById(historico.getIdAgendamento()).ifPresent(agendamento -> {
                    agendamento.setStatus("CONFIRMADO");
                    agendamentoRepository.save(agendamento);
                });

                historico.setStatus("CONFIRMADO");
                historicoRepository.save(historico);

                return true;
            }finally {
                TenantContext.clear();
            }
        }
        return false;
    }
}




