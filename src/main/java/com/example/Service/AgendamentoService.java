package com.example.Service;

import com.example.DTOs.AgendamentoDashboardDTO;
import com.example.DTOs.Asaas.AsaasResponseDTO;
import com.example.DTOs.FaturamentoDTO;
import com.example.DTOs.MovimentacaoDTO;
import com.example.Domain.RoleEnum;
import com.example.Models.*;
import com.example.Multitenancy.TenantContext;
import com.example.Repository.*;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AgendamentoService {

    private static final Logger logger = LoggerFactory.getLogger(AgendamentoService.class);
    private static final int MAX_RECONCILIACAO_LEITURA = 10;
    private static final Set<String> STATUS_PENDENTES_PAGAMENTO = Set.of("PENDENTE", "MENSALISTA_PENDENTE");
    private static final Set<String> STATUS_ASAAS_CONFIRMADOS = Set.of("CONFIRMED", "RECEIVED", "RECEIVED_IN_CASH");
    private static final Set<String> STATUS_ASAAS_CANCELADOS = Set.of("REFUNDED", "REFUND_REQUESTED", "DELETED");

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
    private ContratoMensalistaRepository contratoMensalistaRepository;

    @Autowired
    private AsaasService asaasService;

    @Autowired
    private NotificacaoService notificacaoService;

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

        if (arena.getDiasFuncionamento() != null && !arena.getDiasFuncionamento().isBlank()) {

            String diasConfigurados = arena.getDiasFuncionamento().toLowerCase();
            String diaAtual = traduzirDiaDaSemana(data.getDayOfWeek()).toLowerCase();

            if (!diasConfigurados.contains(diaAtual)) {
                return new ArrayList<>();
            }
        }

        LocalTime abertura = (arena.getHoraInicio() != null) ? arena.getHoraInicio() : LocalTime.of(6, 00);
        LocalTime fechamento = (arena.getHoraFim() != null) ? arena.getHoraFim() : LocalTime.of(23, 00);

        LocalDateTime inicioDia = data.atStartOfDay();
        LocalDateTime fimDia = data.plusDays(1).atStartOfDay();

        List<Agendamentos> agendamentos = agendamentoRepository.findAgendamentosDoDiaComSchema(idQuadra, inicioDia, fimDia, schema);

        // 🔧 Otimização: HashSet para lookup O(1) ao invés de List.contains() O(n)
        Set<LocalTime> horariosOcupados = agendamentos.stream()
                .map(a -> a.getData_inicio().toLocalTime())
                .collect(Collectors.toSet());

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

        validarDiasFuncionamento(arena,booking.getData_inicio().toLocalDate());

        booking.setId_user(user.getIdUser());
        booking.setStatus("PENDENTE");
        booking.setData_fim(booking.getData_inicio().plusHours(1));

        validarDisponibilidade(booking.getId_quadra(), booking.getData_inicio(), booking.getData_fim(), schema, null);

        processarPagamentoAsaas(user, arena, booking);

        Agendamentos salvo = agendamentoRepository.salvarComSchema(booking, schema);
        salvarHistorico(salvo, arena);

        notificacaoService.enviar(
                user.getIdUser().longValue(),
                "Reserva criada para "+ arena.getName()+"! ",
                "Pague sua reserva para confirma-lá!",
                "PENDENTE"
        );

        logger.info("agendamento criado para " + arena.getName()+ " pelo user: " + user.getIdUser());

        return salvo;
    }

    @Transactional
    public Agendamentos updateBookingDate(Integer idAgendamento, LocalDateTime novaDataInicio) {

        String schema = configurarSchema();
        Users user = getUsuarioLogado();
        Arena arena = getArenaAtual(schema);

        Agendamentos booking = agendamentoRepository.buscarPorIdComSchema(idAgendamento, schema)
                .orElseThrow(() -> new RuntimeException("Agendamento não encontrado."));

        validarDiasFuncionamento(arena,novaDataInicio.toLocalDate());

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

        notificacaoService.enviar(
                user.getIdUser().longValue(),
                "Reserva atualizada !",
                "sua reserva foi atualizada para a arena "+ arena.getName() +"!",
                "INFO"
        );

        logger.info("horario alterado para o agendamento "+ idAgendamento+ " da arena: " + arena.getName() + "pelo user:"+user.getIdUser());
        return atualizado;
    }

    private void salvarHistorico(Agendamentos original, Arena arena) {
        try {
            AgendamentoHistorico historico = new AgendamentoHistorico();
            historico.setId_arena(arena.getId());
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

            String schema = arenaRepository.findSchemaNameById((long) arena.getId());

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
        reconciliarPagamentosPendentesDaArenaAtual();
        List<Agendamentos> agendamento = agendamentoRepository.findAllAgendamentos(idQuadra, data, schema);

        if (agendamento.isEmpty()) return new ArrayList<>();

        // 🔧 Otimização N+1: Batch fetch de users e quadras em 2 queries ao invés de 3n
        Set<Integer> userIds = agendamento.stream().map(Agendamentos::getId_user).collect(Collectors.toSet());
        Set<Integer> quadraIds = agendamento.stream().map(Agendamentos::getId_quadra).collect(Collectors.toSet());

        Map<Integer, Users> usersMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(Users::getIdUser, Function.identity()));

        Map<Integer, Quadra> quadrasMap = quadraRepository.findAllById(quadraIds).stream()
                .collect(Collectors.toMap(Quadra::getId, Function.identity()));

        for (Agendamentos a : agendamento) {
            Users user = usersMap.get(a.getId_user());
            a.setNomeCliente(user != null ? user.getNome() : "Usuario não encontrado");

            Quadra quadra = quadrasMap.get(a.getId_quadra());
            a.setQuadraNome(quadra != null ? quadra.getNome() : "Quadra não encontrada");

            String numeroCliente = (user != null && user.getTelefone() != null)
                    ? user.getTelefone() : "";

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

        reconciliarPagamentosPendentesDoUsuario(user.getIdUser());
        List<AgendamentoHistorico> historico = historicoRepository.buscarHistoricoPorUsuario(user.getIdUser());

        // 🔧 Otimização: Limita chamadas HTTP ao Asaas (máx 10 por request)
        // Evita O(n × HTTP) quando há muitos pendentes sem URL
        int maxApiCalls = 10;
        int apiCallCount = 0;

        for (AgendamentoHistorico h : historico) {
            if (apiCallCount >= maxApiCalls) break;

            if (h.getAsaasPaymentId() != null && h.getAsaasInvoiceUrl() == null) {
                try {
                    String urlRecuperada = asaasService.getPaymentUrlById(h.getAsaasPaymentId());
                    apiCallCount++;

                    if (urlRecuperada != null) {
                        h.setAsaasInvoiceUrl(urlRecuperada);
                        historicoRepository.save(h);
                    }
                } catch (Exception e) {
                    logger.warn("Erro ao recuperar URL Asaas para payment {}: {}", h.getAsaasPaymentId(), e.getMessage());
                }
            }
        }

        return historico;
    }

    public int reconciliarPagamentosPendentesDoUsuario(Integer idUser) {
        if (idUser == null) {
            return 0;
        }

        List<AgendamentoHistorico> pendentes = historicoRepository.findPendentesPorUsuario(idUser);
        return reconciliarPagamentosPendentes(pendentes, MAX_RECONCILIACAO_LEITURA);
    }

    public int reconciliarPagamentosPendentesDaArenaAtual() {
        String schema = configurarSchema();
        if (schema == null || schema.isBlank() || "public".equalsIgnoreCase(schema)) {
            return 0;
        }

        Long idArena = arenaRepository.findIdBySchemaName(schema);
        if (idArena == null) {
            return 0;
        }

        List<AgendamentoHistorico> pendentes = historicoRepository.findPendentesPorArena(idArena.intValue());
        return reconciliarPagamentosPendentes(pendentes, MAX_RECONCILIACAO_LEITURA);
    }

    public int reconciliarPagamentosPendentes(List<AgendamentoHistorico> historicos, int limiteChamadas) {
        if (historicos == null || historicos.isEmpty() || limiteChamadas <= 0) {
            return 0;
        }

        Set<String> pagamentosPendentes = new LinkedHashSet<>();
        for (AgendamentoHistorico historico : historicos) {
            if (!isHistoricoPendentePagamento(historico)) {
                continue;
            }

            String paymentId = historico.getAsaasPaymentId();
            if (paymentId != null && !paymentId.isBlank()) {
                pagamentosPendentes.add(paymentId);
            }
        }

        int processados = 0;
        for (String paymentId : pagamentosPendentes) {
            if (processados >= limiteChamadas) {
                break;
            }

            try {
                String statusAsaas = asaasService.checkPaymentStatus(paymentId);
                processados++;

                if (statusAsaas == null || statusAsaas.isBlank()) {
                    continue;
                }

                if (STATUS_ASAAS_CONFIRMADOS.contains(statusAsaas)) {
                    confirmPaymentWebhook(paymentId);
                } else if (STATUS_ASAAS_CANCELADOS.contains(statusAsaas)) {
                    cancelPaymentWebhook(paymentId, "RECONCILIACAO_LEITURA_" + statusAsaas);
                }
            } catch (Exception e) {
                logger.warn("Erro ao reconciliar pagamento {} durante leitura: {}", paymentId, e.getMessage());
            }
        }

        return processados;
    }

    private boolean isHistoricoPendentePagamento(AgendamentoHistorico historico) {
        return historico != null
                && historico.getStatus() != null
                && STATUS_PENDENTES_PAGAMENTO.contains(historico.getStatus().toUpperCase());
    }

    @Transactional
    public void atualizarStatus(Integer idAgendamento, String novoStatus) {
        String schema = configurarSchema();
        String statusAlvo = novoStatus.toUpperCase();

        Long idArena = arenaRepository.findIdBySchemaName(schema);

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

        historicoRepository.buscarPorOrigem(idAgendamento, idArena.intValue()).ifPresent(hist -> {
            hist.setStatus(statusAlvo);
            if (statusAlvo.equals("CANCELADO")) {
                hist.setAsaasInvoiceUrl(null);
            }
            historicoRepository.save(hist);
        });

        if (agendamento.getId_user() != null) {
            Long idClienteDaReserva = agendamento.getId_user().longValue();
            Arena arena = getArenaAtual(schema);

            if (statusAlvo.equals("CANCELADO")) {
                notificacaoService.enviar(
                        idClienteDaReserva,
                        "Reserva cancelada ",
                        "A sua reserva na " + arena.getName() + " foi cancelada.",
                        "CANCELADO"
                );
                logger.info("agendamento do user" + idClienteDaReserva + " foi cancelado para a arena:" + arena.getName());
            }
            else if (statusAlvo.equals("FINALIZADO")) {
                notificacaoService.enviar(
                        idClienteDaReserva,
                        "Reserva Finalizada ",
                        "O seu jogo na " + arena.getName() + " foi concluído. Obrigado por jogar conosco e até a próxima!",
                        "FINALIZADO"
                );
                logger.info("agendamento do user" + idClienteDaReserva + " foi finalizado para a arena:" + arena.getName());

            }
        }
    }

    public boolean confirmPaymentWebhook(String paymentId){
        return confirmPaymentWebhook(paymentId, null);
    }

    public boolean confirmPaymentWebhook(String paymentId, String externalReference){
        List<AgendamentoHistorico> historicosMesmoPagamento = historicoRepository.findAllByAsaasPaymentId(paymentId);

        if (historicosMesmoPagamento != null && historicosMesmoPagamento.stream()
                .anyMatch(h -> h.getStatus() != null && h.getStatus().startsWith("MENSALISTA_"))) {
            return confirmarMensalidadeWebhook(paymentId, historicosMesmoPagamento);
        }

        if ((historicosMesmoPagamento == null || historicosMesmoPagamento.isEmpty())
                && isExternalReferenceMensalista(externalReference)) {
            return confirmarMensalidadePorContrato(paymentId, externalReference);
        }

        if(historicosMesmoPagamento != null && !historicosMesmoPagamento.isEmpty()){
            AgendamentoHistorico historico = historicosMesmoPagamento.get(0);

            if ("CONFIRMADO".equalsIgnoreCase(historico.getStatus()) ||
                    "FINALIZADO".equalsIgnoreCase(historico.getStatus()) ||
                    "CANCELADO".equalsIgnoreCase(historico.getStatus())) {
                System.out.println("Webhook ignorado: Pagamento " + paymentId + " já processado anteriormente.");
                return true;
            }

            Integer id_arena = historico.getId_arena();
            String schema = arenaRepository.findSchemaNameById(id_arena.longValue());
            TenantContext.setCurrentTenant(schema);
            try{
                agendamentoRepository.buscarPorIdComSchema(historico.getIdAgendamento(), schema).ifPresent(agendamento -> {
                    agendamento.setStatus("CONFIRMADO");
                    agendamentoRepository.salvarComSchema(agendamento, schema);

                    if (agendamento.getId_user() != null) {
                        Long userId = agendamento.getId_user().longValue();
                        Arena arena = getArenaAtual(schema);

                        notificacaoService.enviar(
                                userId,
                                "Pagamento Aprovado! ",
                                "O pagamento da sua reserva na "+ arena.getName() +" foi confirmado com sucesso. Bom jogo!",
                                "CONFIRMADO"
                        );

                        logger.info("agendamento do user" + userId + " foi PAGO para a arena:" + arena.getName());
                    }
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

    private boolean isExternalReferenceMensalista(String externalReference) {
        return externalReference != null && externalReference.startsWith("MENSAL_");
    }

    private boolean confirmarMensalidadePorContrato(String paymentId, String externalReference) {
        Integer contratoId = extrairContratoMensalistaId(externalReference);
        if (contratoId == null) {
            return false;
        }

        for (Arena arena : arenaRepository.findByAtivoTrue()) {
            String schema = arena.getSchemaName();
            if (schema == null || schema.isBlank() || "public".equalsIgnoreCase(schema)) {
                continue;
            }

            TenantContext.setCurrentTenant(schema);
            try {
                Optional<ContratoMensalista> contratoOpt = contratoMensalistaRepository.findById(contratoId);
                if (contratoOpt.isEmpty()) {
                    continue;
                }

                ContratoMensalista contrato = contratoOpt.get();
                if (contrato.getAsaasPaymentId() == null || !contrato.getAsaasPaymentId().equals(paymentId)) {
                    continue;
                }

                List<Agendamentos> agendamentos = agendamentoRepository.findByAsaasPaymentIdComSchema(paymentId, schema);
                if (agendamentos.isEmpty()) {
                    logger.warn("Mensalidade {} encontrada, mas sem agendamentos vinculados ao pagamento {} no schema {}.",
                            contratoId, paymentId, schema);
                    return false;
                }

                for (Agendamentos agendamento : agendamentos) {
                    if (!"CANCELADO".equalsIgnoreCase(agendamento.getStatus())
                            && !"FINALIZADO".equalsIgnoreCase(agendamento.getStatus())) {
                        agendamento.setStatus("MENSALISTA_CONFIRMADO");
                        agendamentoRepository.salvarComSchema(agendamento, schema);
                    }

                    atualizarOuCriarHistoricoMensalista(agendamento, arena);
                }

                contrato.setStatus("PAGO");
                contrato.setAtivo(true);
                contratoMensalistaRepository.save(contrato);
                return true;
            } catch (Exception e) {
                logger.error("Erro ao confirmar mensalidade {} pelo externalReference {}: {}",
                        paymentId, externalReference, e.getMessage());
                return false;
            } finally {
                TenantContext.clear();
            }
        }

        return false;
    }

    private Integer extrairContratoMensalistaId(String externalReference) {
        try {
            return Integer.parseInt(externalReference.replace("MENSAL_", ""));
        } catch (Exception e) {
            return null;
        }
    }

    private void atualizarOuCriarHistoricoMensalista(Agendamentos agendamento, Arena arena) {
        historicoRepository.buscarPorOrigem(agendamento.getId_agendamento(), arena.getId())
                .ifPresentOrElse(historico -> {
                    if (!"CANCELADO".equalsIgnoreCase(historico.getStatus())
                            && !"FINALIZADO".equalsIgnoreCase(historico.getStatus())) {
                        historico.setStatus("MENSALISTA_CONFIRMADO");
                        historico.setAsaasPaymentId(agendamento.getAsaasPaymentId());
                        historico.setAsaasInvoiceUrl(agendamento.getAsaasInvoiceUrl());
                        historicoRepository.save(historico);
                    }
                }, () -> salvarHistoricoRecuperadoMensalista(agendamento, arena));
    }

    private void salvarHistoricoRecuperadoMensalista(Agendamentos agendamento, Arena arena) {
        AgendamentoHistorico historico = new AgendamentoHistorico();
        historico.setId_arena(arena.getId());
        historico.setIdUser(agendamento.getId_user());
        historico.setIdAgendamento(agendamento.getId_agendamento());
        historico.setId_quadra(agendamento.getId_quadra());
        historico.setDataInicio(agendamento.getData_inicio());
        historico.setData_fim(agendamento.getData_fim());
        historico.setStatus("MENSALISTA_CONFIRMADO");
        historico.setValor(agendamento.getValor());
        historico.setAsaasPaymentId(agendamento.getAsaasPaymentId());
        historico.setAsaasInvoiceUrl(agendamento.getAsaasInvoiceUrl());
        historico.setArenaName(arena.getName());
        historico.setEnderecoArena(arena.getEndereco() + " - " + arena.getCidade());

        quadraRepository.buscarPorIdComSchema(agendamento.getId_quadra(), arena.getSchemaName())
                .ifPresent(quadra -> historico.setQuadraNome(quadra.getNome()));

        historicoRepository.save(historico);
    }

    private boolean confirmarMensalidadeWebhook(String paymentId, List<AgendamentoHistorico> historicos) {
        if (historicos == null || historicos.isEmpty()) {
            return false;
        }

        boolean algumProcessado = false;
        Set<String> schemasAtualizados = new HashSet<>();

        for (AgendamentoHistorico historico : historicos) {
            if (historico.getStatus() != null &&
                    (historico.getStatus().equalsIgnoreCase("MENSALISTA_CONFIRMADO") ||
                            historico.getStatus().equalsIgnoreCase("FINALIZADO") ||
                            historico.getStatus().equalsIgnoreCase("CANCELADO"))) {
                algumProcessado = true;
                continue;
            }

            Integer idArena = historico.getId_arena();
            if (idArena == null) continue;

            String schema = arenaRepository.findSchemaNameById(idArena.longValue());
            TenantContext.setCurrentTenant(schema);

            try {
                Optional<Agendamentos> agendamentoOpt = agendamentoRepository.buscarPorIdComSchema(historico.getIdAgendamento(), schema);
                if (agendamentoOpt.isPresent()) {
                    Agendamentos agendamento = agendamentoOpt.get();

                    if ("CANCELADO".equalsIgnoreCase(agendamento.getStatus())
                            || "FINALIZADO".equalsIgnoreCase(agendamento.getStatus())) {
                        historico.setStatus(agendamento.getStatus());
                        historicoRepository.save(historico);
                        algumProcessado = true;
                        continue;
                    }

                    agendamento.setStatus("MENSALISTA_CONFIRMADO");
                    agendamentoRepository.salvarComSchema(agendamento, schema);

                    if (agendamento.getId_user() != null) {
                        Long userId = agendamento.getId_user().longValue();
                        Arena arena = getArenaAtual(schema);

                        notificacaoService.enviar(
                                userId,
                                "Pagamento Aprovado! ",
                                "O pagamento da sua mensalidade na " + arena.getName() + " foi confirmado com sucesso.",
                                "MENSALISTA_CONFIRMADO"
                        );
                    }
                }

                historico.setStatus("MENSALISTA_CONFIRMADO");
                historicoRepository.save(historico);

                if (schemasAtualizados.add(schema)) {
                    atualizarContratoMensalistaPorPagamento(paymentId, "PAGO", true);
                }

                algumProcessado = true;
            } catch (Exception e) {
                System.err.println("Erro ao processar webhook mensalista " + paymentId + ": " + e.getMessage());
                return false;
            } finally {
                TenantContext.clear();
            }
        }

        return algumProcessado;
    }

    @Transactional
    public boolean cancelPaymentWebhook(String paymentId, String motivo) {
        if (paymentId == null || paymentId.isBlank()) {
            return false;
        }

        List<AgendamentoHistorico> historicos = historicoRepository.findAllByAsaasPaymentId(paymentId);
        if (historicos == null || historicos.isEmpty()) {
            return false;
        }

        boolean encontrouPagamento = false;
        Set<String> schemasAtualizados = new HashSet<>();

        for (AgendamentoHistorico historico : historicos) {
            Integer idArena = historico.getId_arena();
            if (idArena == null) continue;

            String schema = arenaRepository.findSchemaNameById(idArena.longValue());
            TenantContext.setCurrentTenant(schema);

            try {
                agendamentoRepository.buscarPorIdComSchema(historico.getIdAgendamento(), schema).ifPresent(agendamento -> {
                    if (!"FINALIZADO".equalsIgnoreCase(agendamento.getStatus())) {
                        agendamento.setStatus("CANCELADO");
                        agendamentoRepository.salvarComSchema(agendamento, schema);
                    }
                });

                if (!"FINALIZADO".equalsIgnoreCase(historico.getStatus())) {
                    historico.setStatus("CANCELADO");
                    historicoRepository.save(historico);
                }

                if (schemasAtualizados.add(schema)) {
                    atualizarContratoMensalistaPorPagamento(paymentId, "CANCELADO", false);
                }

                encontrouPagamento = true;
                logger.info("Pagamento {} cancelado/refundado pelo webhook Asaas. Motivo: {}", paymentId, motivo);
            } catch (Exception e) {
                logger.error("Erro ao cancelar agendamentos vinculados ao pagamento {}: {}", paymentId, e.getMessage());
                return false;
            } finally {
                TenantContext.clear();
            }
        }

        return encontrouPagamento;
    }

    private void atualizarContratoMensalistaPorPagamento(String paymentId, String status, boolean ativo) {
        contratoMensalistaRepository.findByAsaasPaymentId(paymentId).ifPresent(contrato -> {
            contrato.setStatus(status);
            contrato.setAtivo(ativo);
            contratoMensalistaRepository.save(contrato);
        });
    }

    private void atualizarHistoricoData(Integer id, String schema, LocalDateTime inicio, LocalDateTime fim) {
        Long idArena = arenaRepository.findIdBySchemaName(schema);

        historicoRepository.buscarPorOrigem(id, idArena.intValue()).ifPresent(hist -> {
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

        } catch (Exception e) {
            throw new RuntimeException("Erro no Asaas: " + e.getMessage());
        }
    }

    public List<AgendamentoDashboardDTO> findStatusForDashboard(){
        String schema = configurarSchema();

        if (schema == null || schema.isEmpty()) {
            throw new IllegalArgumentException("Arena não identificada.");
        }

        reconciliarPagamentosPendentesDaArenaAtual();
        return agendamentoRepository.findAllDashboard(schema);
    }

    public List<FaturamentoDTO> findFaturamentoAnual(int ano){
        String schema = configurarSchema();

        return agendamentoRepository.findFaturamentoAnual(schema,ano);
    }

    public List<MovimentacaoDTO> getUltimasMovimentacoes() {
        String schema = configurarSchema();
        reconciliarPagamentosPendentesDaArenaAtual();
        return agendamentoRepository.findUltimasMovimentacoes(schema);
    }

    private String traduzirDiaDaSemana(java.time.DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY    -> "seg";
            case TUESDAY   -> "ter";
            case WEDNESDAY -> "quar";
            case THURSDAY  -> "qui";
            case FRIDAY    -> "sex";
            case SATURDAY  -> "sab";
            case SUNDAY    -> "dom";
        };
    }

    private void validarDiasFuncionamento(Arena arena, LocalDate data){
        String diasConfig = arena.getDiasFuncionamento();

        if (diasConfig == null || diasConfig.isBlank()) {
            return;
        }

        String diaAtual = traduzirDiaDaSemana(data.getDayOfWeek());

        boolean diaAberto = java.util.Arrays.stream(diasConfig.split(","))
                .map(String::trim)
                .anyMatch(d -> d.equalsIgnoreCase(diaAtual));

        if(!diaAberto){
            throw new IllegalArgumentException("A arena esta fechada neste dia");
        }
    }
}
