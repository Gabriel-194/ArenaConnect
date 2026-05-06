package com.example.Service;

import com.example.Models.AgendamentoHistorico;
import com.example.Models.Agendamentos;
import com.example.Models.Arena;
import com.example.Models.ContratoMensalista;
import com.example.Repository.AgendamentoRepository;
import com.example.Repository.ArenaRepository;
import com.example.Repository.ContratoMensalistaRepository;
import com.example.Repository.HistoricoRepository;
import com.example.Repository.QuadraRepository;
import com.example.Repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgendamentoServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private QuadraRepository quadraRepository;

    @Mock
    private AgendamentoRepository agendamentoRepository;

    @Mock
    private ArenaRepository arenaRepository;

    @Mock
    private HistoricoRepository historicoRepository;

    @Mock
    private ContratoMensalistaRepository contratoMensalistaRepository;

    @Mock
    private AsaasService asaasService;

    @Mock
    private NotificacaoService notificacaoService;

    @InjectMocks
    private AgendamentoService service;

    @Test
    void confirmPaymentWebhookConfirmaTodosMensalistasSemUsarConsultaUnicaPorPaymentId() {
        String paymentId = "pay_mensal_compartilhado";
        Arena arena = new Arena();
        arena.setId(3);
        arena.setName("Arena Teste");
        arena.setSchemaName("arena_teste");

        AgendamentoHistorico historicoPrimeiro = historicoMensalista(101, arena.getId(), paymentId);
        AgendamentoHistorico historicoSegundo = historicoMensalista(102, arena.getId(), paymentId);
        Agendamentos primeiro = agendamentoMensalista(101, paymentId);
        Agendamentos segundo = agendamentoMensalista(102, paymentId);

        lenient().when(historicoRepository.findByAsaasPaymentId(paymentId))
                .thenThrow(new IncorrectResultSizeDataAccessException(1, 2));
        when(historicoRepository.findAllByAsaasPaymentId(paymentId))
                .thenReturn(List.of(historicoPrimeiro, historicoSegundo));
        when(arenaRepository.findSchemaNameById(3L)).thenReturn(arena.getSchemaName());
        when(arenaRepository.findBySchemaName(arena.getSchemaName())).thenReturn(Optional.of(arena));
        when(agendamentoRepository.buscarPorIdComSchema(101, arena.getSchemaName())).thenReturn(Optional.of(primeiro));
        when(agendamentoRepository.buscarPorIdComSchema(102, arena.getSchemaName())).thenReturn(Optional.of(segundo));
        when(contratoMensalistaRepository.findByAsaasPaymentId(paymentId)).thenReturn(Optional.empty());

        boolean confirmado = service.confirmPaymentWebhook(paymentId);

        assertTrue(confirmado);
        assertTrue(List.of(primeiro, segundo).stream()
                .allMatch(a -> "MENSALISTA_CONFIRMADO".equals(a.getStatus())));
        assertTrue(List.of(historicoPrimeiro, historicoSegundo).stream()
                .allMatch(h -> "MENSALISTA_CONFIRMADO".equals(h.getStatus())));
        verify(historicoRepository, never()).findByAsaasPaymentId(paymentId);
        verify(agendamentoRepository).salvarComSchema(eq(primeiro), eq(arena.getSchemaName()));
        verify(agendamentoRepository).salvarComSchema(eq(segundo), eq(arena.getSchemaName()));
    }

    @Test
    void confirmPaymentWebhookConfirmaMensalidadePorExternalReferenceQuandoHistoricoNaoFoiEncontradoPorPagamento() {
        String paymentId = "pay_mensal";
        Arena arena = new Arena();
        arena.setId(3);
        arena.setName("Arena Teste");
        arena.setSchemaName("arena_teste");

        ContratoMensalista contrato = new ContratoMensalista();
        contrato.setId(16);
        contrato.setAsaasPaymentId(paymentId);
        contrato.setStatus("PENDENTE");
        contrato.setAtivo(true);

        Agendamentos primeiro = agendamentoMensalista(101, paymentId);
        Agendamentos segundo = agendamentoMensalista(102, paymentId);

        AgendamentoHistorico historicoPrimeiro = new AgendamentoHistorico();
        historicoPrimeiro.setStatus("MENSALISTA_PENDENTE");
        AgendamentoHistorico historicoSegundo = new AgendamentoHistorico();
        historicoSegundo.setStatus("MENSALISTA_PENDENTE");

        when(historicoRepository.findAllByAsaasPaymentId(paymentId)).thenReturn(Collections.emptyList());
        when(arenaRepository.findByAtivoTrue()).thenReturn(List.of(arena));
        when(contratoMensalistaRepository.findById(16)).thenReturn(Optional.of(contrato));
        when(agendamentoRepository.findByAsaasPaymentIdComSchema(paymentId, arena.getSchemaName()))
                .thenReturn(List.of(primeiro, segundo));
        when(historicoRepository.buscarPorOrigem(101, arena.getId())).thenReturn(Optional.of(historicoPrimeiro));
        when(historicoRepository.buscarPorOrigem(102, arena.getId())).thenReturn(Optional.of(historicoSegundo));

        boolean confirmado = service.confirmPaymentWebhook(paymentId, "MENSAL_16");

        assertTrue(confirmado);
        assertTrue(List.of(primeiro, segundo).stream()
                .allMatch(a -> "MENSALISTA_CONFIRMADO".equals(a.getStatus())));
        assertTrue(List.of(historicoPrimeiro, historicoSegundo).stream()
                .allMatch(h -> "MENSALISTA_CONFIRMADO".equals(h.getStatus())));
        assertTrue("PAGO".equals(contrato.getStatus()));

        verify(agendamentoRepository).salvarComSchema(eq(primeiro), eq(arena.getSchemaName()));
        verify(agendamentoRepository).salvarComSchema(eq(segundo), eq(arena.getSchemaName()));
        verify(contratoMensalistaRepository).save(contrato);
    }

    @Test
    void reconciliarPagamentosPendentesConfirmaMensalistaPagoNoAsaas() {
        String paymentId = "pay_mensal_pago";
        Arena arena = new Arena();
        arena.setId(3);
        arena.setName("Arena Teste");
        arena.setSchemaName("arena_teste");

        AgendamentoHistorico historico = historicoMensalista(101, arena.getId(), paymentId);
        Agendamentos agendamento = agendamentoMensalista(101, paymentId);

        when(asaasService.checkPaymentStatus(paymentId)).thenReturn("CONFIRMED");
        when(historicoRepository.findAllByAsaasPaymentId(paymentId)).thenReturn(List.of(historico));
        when(arenaRepository.findSchemaNameById(3L)).thenReturn(arena.getSchemaName());
        when(arenaRepository.findBySchemaName(arena.getSchemaName())).thenReturn(Optional.of(arena));
        when(agendamentoRepository.buscarPorIdComSchema(101, arena.getSchemaName())).thenReturn(Optional.of(agendamento));
        when(contratoMensalistaRepository.findByAsaasPaymentId(paymentId)).thenReturn(Optional.empty());

        int processados = service.reconciliarPagamentosPendentes(List.of(historico), 10);

        assertTrue(processados == 1);
        assertTrue("MENSALISTA_CONFIRMADO".equals(agendamento.getStatus()));
        assertTrue("MENSALISTA_CONFIRMADO".equals(historico.getStatus()));
        verify(asaasService).checkPaymentStatus(paymentId);
        verify(agendamentoRepository).salvarComSchema(eq(agendamento), eq(arena.getSchemaName()));
    }

    private Agendamentos agendamentoMensalista(Integer id, String paymentId) {
        Agendamentos agendamento = new Agendamentos();
        agendamento.setId_agendamento(id);
        agendamento.setId_user(7);
        agendamento.setId_quadra(11);
        agendamento.setStatus("MENSALISTA_PENDENTE");
        agendamento.setAsaasPaymentId(paymentId);
        agendamento.setAsaasInvoiceUrl("https://asaas.test/" + paymentId);
        agendamento.setData_inicio(LocalDateTime.now().plusDays(1));
        agendamento.setData_fim(LocalDateTime.now().plusDays(1).plusHours(1));
        agendamento.setValor(200.0);
        return agendamento;
    }

    private AgendamentoHistorico historicoMensalista(Integer idAgendamento, Integer idArena, String paymentId) {
        AgendamentoHistorico historico = new AgendamentoHistorico();
        historico.setIdAgendamento(idAgendamento);
        historico.setId_arena(idArena);
        historico.setIdUser(7);
        historico.setStatus("MENSALISTA_PENDENTE");
        historico.setAsaasPaymentId(paymentId);
        return historico;
    }
}
