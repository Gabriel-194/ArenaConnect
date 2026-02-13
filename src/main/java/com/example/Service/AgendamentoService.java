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
    public Agendamentos createBooking(Agendamentos booking) {
        String schema = configurarSchema();
        Users user = getUsuarioLogado();
        Arena arena = getArenaAtual(schema);

        booking.setId_user(user.getIdUser());
        booking.setStatus("PENDENTE");
        booking.setData_fim(booking.getData_inicio().plusHours(1));

        validarDisponibilidade(booking.getId_quadra(), booking.getData_inicio(), booking.getData_fim(), schema, null);

        processarPagamentoAsaas(user, arena, booking);

        Agendamentos salvo = agendamentoRepository.salvarComSchema(booking, schema);
        salvarHistorico(salvo, schema, arena);

        return salvo;
    }

    @Transactional
    public Agendamentos updateBookingDate(Integer idAgendamento, LocalDateTime novaDataInicio) {
        String schema = configurarSchema();
        Users user = getUsuarioLogado();

        Agendamentos booking = agendamentoRepository.buscarPorIdComSchema(idAgendamento, schema)
                .orElseThrow(() -> new RuntimeException("Agendamento não encontrado."));

        validarPermissaoEdicao(user, booking);

        if ("CANCELADO".equals(booking.getStatus()) || "FINALIZADO".equals(booking.getStatus())) {
            throw new IllegalArgumentException("Status inválido para edição.");
        }

        LocalDateTime novaDataFim = novaDataInicio.plusHours(1);

        validarDisponibilidade(booking.getId_quadra(), novaDataInicio, novaDataFim, schema, idAgendamento);

        booking.setData_inicio(novaDataInicio);
        booking.setData_fim(novaDataFim);

        Agendamentos atualizado = agendamentoRepository.salvarComSchema(booking, schema);
        atualizarHistoricoData(idAgendamento, schema, novaDataInicio, novaDataFim);

        return atualizado;
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

            String numeroCliente = userRepository.findById(a.getId_user())

                    .map(user -> user.getTelefone())
                    .orElse("usuario nao encontrado");

            numeroCliente = numeroCliente.replaceAll("\\D", "");
            if (numeroCliente.length() == 11) {
                numeroCliente = numeroCliente.replaceFirst(
                        "(\\d{2})(\\d{5})(\\d{4})",
                        "($1) $2-$3"
                );
            }

            a.setNumeroCliente(numeroCliente);

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

            if ("CONFIRMADO".equalsIgnoreCase(historico.getStatus()) ||
                    "FINALIZADO".equalsIgnoreCase(historico.getStatus())) {
                System.out.println("Webhook ignorado: Pagamento " + paymentId + " já processado anteriormente.");
                return true;
            }

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
            }catch (Exception e) {
                System.err.println("Erro ao processar webhook: " + e.getMessage());
                return false;
            } finally {
                TenantContext.clear();
            }
        }
        System.out.println("Webhook ignorado: Pagamento " + paymentId + " não encontrado no sistema.");
        return false;
    }

    private void atualizarHistoricoData(Integer id, String schema, LocalDateTime inicio, LocalDateTime fim) {
        historicoRepository.buscarPorOrigem(id, schema).ifPresent(hist -> {
            hist.setDataInicio(inicio);
            hist.setData_fim(fim);
            historicoRepository.save(hist);
        });
    }

    private Users getUsuarioLogado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Usuário não logado."));
    }

    private Arena getArenaAtual(String schema) {
        return arenaRepository.findBySchemaName(schema).orElseThrow(() -> new RuntimeException("Arena não encontrada."));
    }

    private void validarPermissaoEdicao(Users user, Agendamentos booking) {
        boolean isAdmin = user.getRole() == RoleEnum.ADMIN || user.getRole() == RoleEnum.SUPERADMIN;
        if (!isAdmin && !booking.getId_user().equals(user.getIdUser())) {
            throw new SecurityException("Sem permissão para editar este agendamento.");
        }
    }

    private void validarDisponibilidade(Integer quadraId, LocalDateTime inicio, LocalDateTime fim, String schema, Integer idIgnorar) {
        List<Agendamentos> conflitos = agendamentoRepository.findAgendamentosDoDiaComSchema(quadraId, inicio, fim, schema);

        boolean ocupado = conflitos.stream().anyMatch(a ->
                !a.getId_agendamento().equals(idIgnorar) &&
                        a.getData_inicio().equals(inicio) &&
                        !"CANCELADO".equals(a.getStatus())
        );

        if (ocupado) {
            throw new IllegalArgumentException("Horário indisponível!");
        }
    }

    private void processarPagamentoAsaas(Users user, Arena arena, Agendamentos booking) {
        if (arena.getAsaasWalletId() == null) {
            throw new RuntimeException("Arena não configurada para receber pagamentos.");
        }

        if (user.getAsaasCustomerId() == null) {
            String customerId = asaasService.createCustomer(user);
            user.setAsaasCustomerId(customerId);
            userRepository.save(user);
        }

        try {
            AsaasResponseDTO cobranca = asaasService.createPaymentWithSplit(
                    user.getAsaasCustomerId(),
                    booking.getValor(),
                    arena.getAsaasWalletId()
            );
            booking.setAsaasPaymentId(cobranca.getId());
            booking.setAsaasInvoiceUrl(cobranca.getInvoiceUrl());

            if (cobranca.getPix() != null) {
                booking.setPixQrCode(cobranca.getPix().getEncodedImage());
                booking.setPixCopyPaste(cobranca.getPix().getPayload());
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro no Asaas: " + e.getMessage());
        }
    }
}




