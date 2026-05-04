package com.example.Service;

import com.example.DTOs.Asaas.AsaasResponseDTO;
import com.example.Models.AgendamentoHistorico;
import com.example.Models.Agendamentos;
import com.example.Models.Arena;
import com.example.Models.ContratoMensalista;
import com.example.Models.Quadra;
import com.example.Models.Users;
import com.example.Repository.AgendamentoRepository;
import com.example.Repository.ArenaRepository;
import com.example.Repository.ContratoMensalistaRepository;
import com.example.Repository.HistoricoRepository;
import com.example.Repository.QuadraRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ContratoMensalistaService {

    @Autowired
    private ContratoMensalistaRepository contratoRepository;

    @Autowired
    private AsaasService asaasService;

    @Autowired
    private QuadraRepository quadraRepository;

    @Autowired
    private ArenaRepository arenaRepository;

    @Autowired
    private HistoricoRepository historicoRepository;

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    @Transactional
    public synchronized ContratoMensalista criarAssinaturaMensalista(
            Users user,
            Arena arena,
            Integer idQuadra,
            int diaSemanaNumero,
            LocalTime horaInicio,
            LocalTime horaFim
    ) {
        LocalDate primeiraData = proximaDataDoDiaSemana(LocalDate.now(), DayOfWeek.of(diaSemanaNumero));
        return criarAssinaturaMensalista(user, arena, idQuadra, diaSemanaNumero, primeiraData, horaInicio, horaFim);
    }

    @Transactional
    public ContratoMensalista criarAssinaturaMensalista(
            Users user,
            Arena arena,
            Integer idQuadra,
            int diaSemanaNumero,
            LocalDate dataInicio,
            LocalTime horaInicio,
            LocalTime horaFim
    ) {
        Quadra quadra = quadraRepository.findById(idQuadra)
                .orElseThrow(() -> new RuntimeException("Quadra nao encontrada"));

        DayOfWeek diaEscolhido = DayOfWeek.of(diaSemanaNumero);
        validarDadosMensalista(arena, dataInicio, horaInicio, horaFim, diaEscolhido);
        asaasService.validarWalletSplitArena(arena.getAsaasWalletId());

        List<LocalDate> datasDaMensalidade = calcularDatasRestantesDoMes(dataInicio, diaEscolhido);
        int quantidadeJogosRestantes = datasDaMensalidade.size();

        if (quantidadeJogosRestantes == 0) {
            throw new IllegalArgumentException("Nao ha mais dias disponiveis para este dia da semana neste mes. Aguarde o mes seguinte.");
        }

        double percentualDesconto = arena.getDescontoMensalista() != null ? arena.getDescontoMensalista() : 0.0;
        double valorBrutoTotal = arredondarMoeda(quadra.getValor_hora() * quantidadeJogosRestantes);
        double valorTotalComDesconto = arredondarMoeda(valorBrutoTotal - (valorBrutoTotal * (percentualDesconto / 100.0)));
        double valorPorJogoComDesconto = arredondarMoeda(valorTotalComDesconto / quantidadeJogosRestantes);

        validarDisponibilidadeMensalista(idQuadra, datasDaMensalidade, horaInicio, horaFim, arena.getSchemaName());

        AsaasResponseDTO asaasResponse = null;

        try {
            ContratoMensalista contrato = new ContratoMensalista();
            contrato.setIdUser(user.getIdUser());
            contrato.setIdQuadra(idQuadra);
            contrato.setDiaSemana(diaSemanaNumero);
            contrato.setHoraInicio(horaInicio);
            contrato.setHoraFim(horaFim);
            contrato.setValorPactuado(valorTotalComDesconto);
            contrato.setStatus("PENDENTE");
            contrato.setAtivo(true);

            contrato = contratoRepository.save(contrato);

            String externalReference = "MENSAL_" + contrato.getId();
            String descricao = "Mensalidade ArenaConnect - " + quantidadeJogosRestantes + " jogos restantes para este mes.";

            asaasResponse = asaasService.createPaymentWithSplit(
                    valorTotalComDesconto,
                    user,
                    arena,
                    descricao,
                    externalReference
            );

            validarRespostaAsaas(asaasResponse);

            contrato.setAsaasPaymentId(asaasResponse.getId());
            contrato.setAsaasInvoiceUrl(asaasResponse.getInvoiceUrl());

            validarDisponibilidadeMensalista(idQuadra, datasDaMensalidade, horaInicio, horaFim, arena.getSchemaName());
            criarAgendamentosMensalistas(
                    user,
                    arena,
                    quadra,
                    datasDaMensalidade,
                    horaInicio,
                    horaFim,
                    valorPorJogoComDesconto,
                    asaasResponse
            );

            return contratoRepository.save(contrato);
        } catch (RuntimeException e) {
            cancelarCobrancaCriada(asaasResponse);
            throw e;
        }
    }

    private void validarDadosMensalista(Arena arena, LocalDate dataInicio, LocalTime horaInicio, LocalTime horaFim, DayOfWeek diaEscolhido) {
        if (arena == null || arena.getSchemaName() == null || arena.getSchemaName().isBlank()) {
            throw new IllegalArgumentException("Arena invalida para assinatura mensalista.");
        }

        if (dataInicio == null) {
            throw new IllegalArgumentException("Data inicial e obrigatoria para assinatura mensalista.");
        }

        if (dataInicio.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Data inicial nao pode estar no passado.");
        }

        if (horaInicio == null || horaFim == null || !horaFim.isAfter(horaInicio)) {
            throw new IllegalArgumentException("Horario invalido para assinatura mensalista.");
        }

        if (dataInicio.getDayOfWeek() != diaEscolhido) {
            throw new IllegalArgumentException("A data inicial nao corresponde ao dia da semana selecionado.");
        }
    }

    private LocalDate proximaDataDoDiaSemana(LocalDate inicio, DayOfWeek diaEscolhido) {
        LocalDate data = inicio;
        while (data.getDayOfWeek() != diaEscolhido) {
            data = data.plusDays(1);
        }
        return data;
    }

    private List<LocalDate> calcularDatasRestantesDoMes(LocalDate dataInicio, DayOfWeek diaEscolhido) {
        List<LocalDate> datas = new ArrayList<>();
        LocalDate data = dataInicio;
        LocalDate fimDoMes = YearMonth.from(dataInicio).atEndOfMonth();

        while (!data.isAfter(fimDoMes)) {
            if (data.getDayOfWeek() == diaEscolhido) {
                datas.add(data);
            }
            data = data.plusDays(1);
        }

        return datas;
    }

    private double arredondarMoeda(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    private void validarRespostaAsaas(AsaasResponseDTO asaasResponse) {
        if (asaasResponse == null || asaasResponse.getId() == null || asaasResponse.getId().isBlank()) {
            throw new IllegalStateException("Asaas nao retornou uma cobranca valida para a mensalidade.");
        }
    }

    private void cancelarCobrancaCriada(AsaasResponseDTO asaasResponse) {
        if (asaasResponse == null || asaasResponse.getId() == null || asaasResponse.getId().isBlank()) {
            return;
        }

        try {
            asaasService.cancelarCobranca(asaasResponse.getId());
        } catch (Exception cancelamentoErro) {
            System.err.println("Aviso: falha ao cancelar cobranca Asaas apos erro local: " + cancelamentoErro.getMessage());
        }
    }

    private void validarDisponibilidadeMensalista(
            Integer idQuadra,
            List<LocalDate> datas,
            LocalTime horaInicio,
            LocalTime horaFim,
            String schema
    ) {
        for (LocalDate data : datas) {
            LocalDateTime inicio = data.atTime(horaInicio);
            LocalDateTime fim = data.atTime(horaFim);

            List<Agendamentos> conflitos = agendamentoRepository.findAgendamentosDoDiaComSchema(idQuadra, inicio, fim, schema);

            boolean ocupado = conflitos.stream().anyMatch(agendamento ->
                    !"CANCELADO".equalsIgnoreCase(agendamento.getStatus())
                            && agendamento.getData_inicio() != null
                            && agendamento.getData_fim() != null
                            && agendamento.getData_inicio().isBefore(fim)
                            && agendamento.getData_fim().isAfter(inicio)
            );

            if (ocupado) {
                throw new IllegalArgumentException("Horario indisponivel em " + data + ".");
            }
        }
    }

    private void criarAgendamentosMensalistas(
            Users user,
            Arena arena,
            Quadra quadra,
            List<LocalDate> datas,
            LocalTime horaInicio,
            LocalTime horaFim,
            double valorPorJogo,
            AsaasResponseDTO asaasResponse
    ) {
        String schema = arena.getSchemaName();

        for (LocalDate data : datas) {
            Agendamentos agendamento = new Agendamentos();
            agendamento.setId_user(user.getIdUser());
            agendamento.setId_quadra(quadra.getId());
            agendamento.setData_inicio(data.atTime(horaInicio));
            agendamento.setData_fim(data.atTime(horaFim));
            agendamento.setStatus("MENSALISTA_PENDENTE");
            agendamento.setValor(valorPorJogo);
            agendamento.setAsaasPaymentId(asaasResponse.getId());
            agendamento.setAsaasInvoiceUrl(asaasResponse.getInvoiceUrl());

            Agendamentos salvo = agendamentoRepository.salvarComSchema(agendamento, schema);
            salvarHistoricoMensalista(salvo, arena, quadra);
        }
    }

    private void salvarHistoricoMensalista(Agendamentos agendamento, Arena arena, Quadra quadra) {
        AgendamentoHistorico historico = new AgendamentoHistorico();
        historico.setId_arena(arena.getId());
        historico.setIdUser(agendamento.getId_user());
        historico.setIdAgendamento(agendamento.getId_agendamento());
        historico.setId_quadra(agendamento.getId_quadra());
        historico.setDataInicio(agendamento.getData_inicio());
        historico.setData_fim(agendamento.getData_fim());
        historico.setStatus(agendamento.getStatus());
        historico.setValor(agendamento.getValor());
        historico.setAsaasPaymentId(agendamento.getAsaasPaymentId());
        historico.setAsaasInvoiceUrl(agendamento.getAsaasInvoiceUrl());
        historico.setArenaName(arena.getName());
        historico.setQuadraNome(quadra.getNome());
        historico.setEnderecoArena(arena.getEndereco() + " - " + arena.getCidade());

        historicoRepository.save(historico);
    }

    /**
     * Otimizacao: pre-filtra arenas pelo historico do usuario.
     * Antes: iterava todas as arenas do sistema, O(A_total) queries.
     * Agora: identifica quais arenas o user ja usou via historico, O(A_user) queries.
     */
    public List<ContratoMensalista> listarMeusContratos(Users user) {
        List<AgendamentoHistorico> historicos = historicoRepository.buscarHistoricoPorUsuario(user.getIdUser());

        Set<Integer> arenaIdsDoUser = historicos.stream()
                .map(AgendamentoHistorico::getId_arena)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<Arena> arenas;
        if (arenaIdsDoUser.isEmpty()) {
            arenas = arenaRepository.findAll();
        } else {
            arenas = arenaRepository.findAllById(
                    arenaIdsDoUser.stream().map(Integer::longValue).collect(Collectors.toList())
            );
        }

        Set<ContratoMensalista> contratosUnicos = new LinkedHashSet<>();

        for (Arena arena : arenas) {
            if (!arena.isAtivo() || arena.getSchemaName() == null || "public".equals(arena.getSchemaName())) {
                continue;
            }

            try {
                List<ContratoMensalista> contratosArena = contratoRepository.findByIdUserComSchema(
                        user.getIdUser(), arena.getSchemaName()
                );

                for (ContratoMensalista contrato : contratosArena) {
                    contrato.setIdArena(arena.getId());
                    contrato.setArenaName(arena.getName());
                    contratosUnicos.add(contrato);
                }

            } catch (Exception e) {
                System.err.println("Aviso: Falha ao buscar na arena " + arena.getName() + " - " + e.getMessage());
            }
        }

        return new ArrayList<>(contratosUnicos);
    }
}
