//package com.example.Service;
//
//import com.example.DTOs.Asaas.AsaasCustumerDTO;
//import com.example.DTOs.Asaas.AsaasPaymentDTO;
//import com.example.DTOs.Asaas.AsaasResponseDTO;
//import com.example.DTOs.Asaas.AsaasSplitDTO;
//import com.example.Domain.RoleEnum;
//import com.example.Models.*;
//import com.example.Multitenancy.TenantContext;
//import com.example.Repository.*;
//import jakarta.transaction.Transactional;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.time.LocalTime;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Service
//public class AgendamentoService {
//
//    @Autowired
//    private UserRepository userRepository;
//
//    @Autowired
//    private QuadraRepository quadraRepository;
//
//    @Autowired
//    private AgendamentoRepository agendamentoRepository;
//
//    @Autowired
//    private ArenaRepository arenaRepository;
//
//    @Autowired
//    private HistoricoRepository historicoRepository;
//
//    @Autowired
//    private AsaasService asaasService;
//
//    private String configurarSchema() {
//        String currentTenant = TenantContext.getCurrentTenant();
//
//        if (currentTenant != null && !currentTenant.isEmpty()) {
//            return currentTenant;
//        } else {
//            return "public";
//        }
//    }
//
//    @Transactional
//    public List<LocalTime> getHorariosDisponiveis(Integer idQuadra, LocalDate data) {
//        String schema = configurarSchema();
//
//        Arena arena = arenaRepository.findBySchemaName(schema)
//                .orElseThrow(() -> new RuntimeException("Arena não encontrada para o schema: " + schema));
//
//        LocalTime abertura = (arena.getHoraInicio() != null) ? arena.getHoraInicio() : LocalTime.of(6, 00);
//        LocalTime fechamento = (arena.getHoraFim() != null) ? arena.getHoraFim() : LocalTime.of(23, 00);
//
//        LocalDateTime inicioDia = data.atStartOfDay();
//        LocalDateTime fimDia = data.plusDays(1).atStartOfDay();
//
//        List<Agendamentos> agendamentos = agendamentoRepository.findAgendamentosDoDiaComSchema(idQuadra, inicioDia, fimDia, schema);
//
//        List<LocalTime> horariosOcupados = agendamentos.stream()
//                .map(a -> a.getData_inicio().toLocalTime())
//                .collect(Collectors.toList());
//
//        List<LocalTime> horariosDisponiveis = new ArrayList<>();
//        LocalTime atual = abertura;
//
//        while (atual.isBefore(fechamento)) {
//            if (!horariosOcupados.contains(atual)) {
//                horariosDisponiveis.add(atual);
//            }
//            atual = atual.plusHours(1);
//        }
//        return horariosDisponiveis;
//    }
//
//    @Transactional
//    public Agendamentos createBooking(Agendamentos newBooking) {
//
//        // 🔐 Usuário logado
//        String emailUsuarioLogado = SecurityContextHolder.getContext()
//                .getAuthentication()
//                .getName();
//
//        Users currentUser = userRepository.findByEmail(emailUsuarioLogado)
//                .orElseThrow(() -> new RuntimeException("Usuário logado não encontrado."));
//
//        // 🏷️ Schema atual (tenant)
//        String currentSchema = configurarSchema();
//
//        // ✏️ EDIÇÃO de agendamento existente
//        if (newBooking.getId_agendamento() != null) {
//
//            Agendamentos agendamentoOriginal =
//                    agendamentoRepository.buscarPorIdComSchema(
//                            newBooking.getId_agendamento(),
//                            currentSchema
//                    ).orElseThrow(() ->
//                            new RuntimeException("Agendamento não encontrado para edição.")
//                    );
//
//            boolean isOwner =
//                    agendamentoOriginal.getId_user().equals(currentUser.getIdUser());
//            boolean isAdmin =
//                    currentUser.getRole() == RoleEnum.ADMIN;
//
//            if (!isOwner && !isAdmin) {
//                throw new SecurityException(
//                        "Você não tem permissão para editar este agendamento."
//                );
//            }
//
//            // mantém o dono original
//            newBooking.setId_user(agendamentoOriginal.getId_user());
//
//            if (newBooking.getStatus() == null) {
//                newBooking.setStatus(agendamentoOriginal.getStatus());
//            }
//
//        } else {
//            // 🆕 NOVO agendamento
//            newBooking.setId_user(currentUser.getIdUser());
//        }
//
//        // ✅ Validações básicas
//        if (newBooking.getId_quadra() == null) {
//            throw new IllegalArgumentException("ID da quadra não pode ser nulo");
//        }
//
//        if (newBooking.getData_inicio() == null) {
//            throw new IllegalArgumentException("Data de início não pode ser nula");
//        }
//
//        LocalDate data = newBooking.getData_inicio().toLocalDate();
//        LocalTime horario = newBooking.getData_inicio().toLocalTime();
//
//        List<LocalTime> horariosDisponiveis =
//                getHorariosDisponiveis(newBooking.getId_quadra(), data);
//
//        if (!horariosDisponiveis.contains(horario)) {
//            throw new IllegalArgumentException(
//                    "Horário não está disponível para agendamento"
//            );
//        }
//
//
//        newBooking.setData_fim(newBooking.getData_inicio().plusHours(1));
//
//        if (newBooking.getId_agendamento() == null) {
//
//            Arena arena = arenaRepository.findBySchemaName(currentSchema)
//                    .orElseThrow(() ->
//                            new RuntimeException("Arena não encontrada")
//                    );
//
//            if (arena.getAsaasWalletId() == null) {
//                throw new RuntimeException(
//                        "Arena não possui carteira Asaas configurada"
//                );
//            }
//
//            // 1️⃣ Criar / buscar customer no Asaas
//            String asaasCustomerId =
//                    getOrCreateAsaasCustomer(currentUser);
//
//            // 2️⃣ Cálculo do split
//            Double valorTotal = newBooking.getValor();
//            Double percentualArenaConnect = 20.0;
//            Double percentualArena = 80.0;
//
//            // 3️⃣ Montar cobrança
//            AsaasPaymentDTO paymentDTO = new AsaasPaymentDTO();
//            paymentDTO.setCustomer(asaasCustomerId);
//            paymentDTO.setBillingType("PIX");
//            paymentDTO.setValue(valorTotal);
//            paymentDTO.setDueDate(
//                    newBooking.getData_inicio()
//                            .toLocalDate()
//                            .toString()
//            );
//            paymentDTO.setDescription(
//                    "Reserva de quadra - " + arena.getName()
//            );
//            paymentDTO.setExternalReference(
//                    currentSchema + ":" + currentUser.getIdUser()
//            );
//
//            // 4️⃣ Split arena
//            AsaasSplitDTO splitArena = new AsaasSplitDTO();
//            splitArena.setWalletId(arena.getAsaasWalletId());
//            splitArena.setPercentualValue(percentualArena);
//            splitArena.setDescription("Pagamento para arena");
//
//            // 5️⃣ Split ArenaConnect
//            AsaasSplitDTO splitMaster = new AsaasSplitDTO();
//            splitMaster.setWalletId(arena.getAsaasWalletId());
//            splitMaster.setPercentualValue(percentualArenaConnect);
//            splitMaster.setDescription("Taxa ArenaConnect");
//
//            paymentDTO.setSplit(
//                    Arrays.asList(splitArena, splitMaster)
//            );
//
//            // 6️⃣ Criar cobrança no Asaas
//            AsaasResponseDTO asaasResponse =
//
//
//            // 7️⃣ Persistir dados de pagamento
//            newBooking.setAsaasPaymentId(asaasResponse.getId());
//            newBooking.setAsaasInvoiceUrl(
//                    asaasResponse.getInvoiceUrl()
//            );
//
//            if (asaasResponse.getPix() != null) {
//                newBooking.setPixQrCode(
//                        asaasResponse.getPix().getEncodedImage()
//                );
//                newBooking.setPixCopyPaste(
//                        asaasResponse.getPix().getPayload()
//                );
//            }
//
//            newBooking.setStatus("PENDENTE_PAGAMENTO");
//
//        } else if (newBooking.getStatus() == null) {
//            newBooking.setStatus("PENDENTE");
//        }
//
//        // 💾 Salvar no schema correto
//        Agendamentos savedBooking =
//                agendamentoRepository.salvarComSchema(
//                        newBooking,
//                        currentSchema
//                );
//
//        salvarHistorico(savedBooking, currentSchema);
//
//        return savedBooking;
//    }
//    private String getOrCreateAsaasCustomer(Users user) {
//
//        AsaasCustumerDTO customerDTO = new AsaasCustumerDTO();
//        customerDTO.setName(user.getNome());
//        customerDTO.setCpfCnpj(user.getCpf());
//        customerDTO.setEmail(user.getEmail());
//        customerDTO.setPhone(user.getTelefone());
//        customerDTO.setMobilePhone(user.getTelefone());
//        customerDTO.setExternalReference("USER_" + user.getIdUser());
//
//
//    }
//
//
//
//    private void salvarHistorico(Agendamentos original, String schema) {
//        try {
//            AgendamentoHistorico historico = historicoRepository
//                    .findBySchemaNameAndIdAgendamento(schema, original.getId_agendamento())
//                    .orElse(new AgendamentoHistorico());
//
//            historico.setSchemaName(schema);
//
//            historico.setIdUser(original.getId_user());
//            historico.setIdAgendamento(original.getId_agendamento());
//            historico.setId_quadra(original.getId_quadra());
//            historico.setDataInicio(original.getData_inicio());
//            historico.setData_fim(original.getData_fim());
//            historico.setStatus(original.getStatus());
//            historico.setValor(original.getValor());
//
//            arenaRepository.findBySchemaName(schema).ifPresent(arena -> {
//                historico.setArenaName(arena.getName());
//                historico.setEnderecoArena(arena.getEndereco() + " - " + arena.getCidade());
//            });
//
//            quadraRepository.buscarPorIdComSchema(original.getId_quadra(), schema).ifPresent(quadra -> {
//                historico.setQuadraNome(quadra.getNome());
//            });
//
//            historicoRepository.save(historico);
//
//        } catch (Exception e) {
//            System.err.println("Erro ao salvar histórico: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//
//    @Transactional
//    public List<Agendamentos> findAllAgendamentos(Integer idQuadra, LocalDate data) {
//        String schema = configurarSchema();
//
//        if (schema == null || schema.isEmpty()) {
//            throw new IllegalArgumentException("O identificador da arena (schema) é obrigatório.");
//        }
//        List<Agendamentos> agendamento = agendamentoRepository.findAllAgendamentos(idQuadra, data, schema);
//
//        for (Agendamentos a : agendamento) {
//            String nomeCliente = userRepository.findById(a.getId_user())
//                    .map(user -> user.getNome())
//                    .orElse("Usuario não encontrado");
//
//            a.setNomeCliente(nomeCliente);
//
//            String nomeQuadra = quadraRepository.findById(a.getId_quadra())
//                    .map(quadra -> quadra.getNome())
//                    .orElse("Quadra não encontrada");
//
//            a.setQuadraNome(nomeQuadra);
//        }
//
//        return new ArrayList<>(agendamento);
//    }
//
//    @Transactional
//    public List<AgendamentoHistorico> findAgendamentosClients() {
//        String email = SecurityContextHolder.getContext().getAuthentication().getName();
//
//        Users user = userRepository.findByEmail(email)
//                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));
//
//        return historicoRepository.buscarHistoricoPorUsuario(user.getIdUser());
//    }
//
//    @Transactional
//    public void atualizarStatus(Integer idAgendamento, String novoStatus) {
//        String schema = configurarSchema();
//        String statusAlvo = novoStatus.toUpperCase();
//
//        String email = SecurityContextHolder.getContext().getAuthentication().getName();
//        Users usuarioLogado = userRepository.findByEmail(email)
//                .orElseThrow(() -> new RuntimeException("Usuário não autenticado."));
//
//        boolean isAdmin = usuarioLogado.getRole() == RoleEnum.ADMIN || usuarioLogado.getRole() == RoleEnum.SUPERADMIN;
//
//        Agendamentos agendamento = agendamentoRepository.buscarPorIdComSchema(idAgendamento, schema)
//                .orElseThrow(() -> new RuntimeException("Agendamento não encontrado."));
//
//
//        if (!isAdmin) {
//            if (!agendamento.getId_user().equals(usuarioLogado.getIdUser())) {
//                throw new SecurityException("Você não tem permissão para alterar este agendamento.");
//            }
//
//            if (statusAlvo.equals("FINALIZADO") || statusAlvo.equals("CONFIRMADO")) {
//                throw new SecurityException("Ação não permitida. Aguarde a confirmação do pagamento.");
//            }
//
//            if (!statusAlvo.equals("CANCELADO")) {
//                throw new SecurityException("Status inválido para operação de cliente.");
//            }
//
//        }
//
//        agendamento.setStatus(statusAlvo);
//        agendamentoRepository.salvarComSchema(agendamento, schema);
//
//        historicoRepository.buscarPorOrigem(idAgendamento, schema).ifPresent(hist -> {
//            hist.setStatus(statusAlvo);
//            historicoRepository.save(hist);
//        });
//    }
//
//
//}

