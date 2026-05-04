package com.example.Service;

import com.example.DTOs.Asaas.AsaasResponseDTO;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContratoMensalistaServiceTest {

    @Mock
    private ContratoMensalistaRepository contratoRepository;

    @Mock
    private AsaasService asaasService;

    @Mock
    private QuadraRepository quadraRepository;

    @Mock
    private ArenaRepository arenaRepository;

    @Mock
    private HistoricoRepository historicoRepository;

    @Mock
    private AgendamentoRepository agendamentoRepository;

    @InjectMocks
    private ContratoMensalistaService service;

    @Test
    void criarAssinaturaMensalistaCriaOcorrenciasRestantesDoMesComValorTotalComDesconto() {
        Users user = new Users();
        user.setIdUser(7);
        user.setNome("Cliente Mensal");
        user.setEmail("cliente@email.com");
        user.setCpf("12345678901");
        user.setTelefone("41999999999");
        user.setAsaasCustomerId("cus_123");

        Arena arena = new Arena();
        arena.setId(3);
        arena.setName("Arena Teste");
        arena.setSchemaName("arena_teste");
        arena.setAsaasWalletId("wallet_123");
        arena.setDescontoMensalista(10.0);

        Quadra quadra = new Quadra();
        quadra.setId(11);
        quadra.setNome("Quadra 1");
        quadra.setValor_hora(100.0);

        LocalDate primeiraData = proximaDataComMinimoDeOcorrencias(3);
        int diaSemana = primeiraData.getDayOfWeek().getValue();
        int quantidadeOcorrencias = contarOcorrenciasAteFimDoMes(primeiraData);

        ContratoMensalista contratoComId = new ContratoMensalista();
        contratoComId.setId(44);
        when(quadraRepository.findById(quadra.getId())).thenReturn(Optional.of(quadra));
        when(contratoRepository.save(any(ContratoMensalista.class)))
                .thenReturn(contratoComId)
                .thenAnswer(invocation -> invocation.getArgument(0));

        AsaasResponseDTO asaasResponse = new AsaasResponseDTO();
        asaasResponse.setId("pay_123");
        asaasResponse.setInvoiceUrl("https://asaas.test/pay_123");
        when(asaasService.createPaymentWithSplit(
                eq(quantidadeOcorrencias * 90.0),
                eq(user),
                eq(arena),
                any(String.class),
                eq("MENSAL_44")
        )).thenReturn(asaasResponse);
        when(agendamentoRepository.findAgendamentosDoDiaComSchema(
                eq(quadra.getId()),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                eq(arena.getSchemaName())
        )).thenReturn(Collections.emptyList());
        when(agendamentoRepository.salvarComSchema(any(Agendamentos.class), eq(arena.getSchemaName())))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.criarAssinaturaMensalista(
                user,
                arena,
                quadra.getId(),
                diaSemana,
                primeiraData,
                LocalTime.of(20, 0),
                LocalTime.of(21, 0)
        );

        ArgumentCaptor<Agendamentos> agendamentoCaptor = ArgumentCaptor.forClass(Agendamentos.class);
        verify(agendamentoRepository, times(quantidadeOcorrencias))
                .salvarComSchema(agendamentoCaptor.capture(), eq(arena.getSchemaName()));

        List<Agendamentos> agendamentos = agendamentoCaptor.getAllValues();
        assertEquals(primeiraData.atTime(20, 0), agendamentos.get(0).getData_inicio());
        assertTrue(agendamentos.stream().allMatch(a -> "MENSALISTA_PENDENTE".equals(a.getStatus())));
        assertTrue(agendamentos.stream().allMatch(a -> a.getValor().equals(90.0)));
        assertTrue(agendamentos.stream().allMatch(a -> "pay_123".equals(a.getAsaasPaymentId())));
    }

    @Test
    void criarAssinaturaMensalistaRejeitaDataInicialNoPassado() {
        Users user = criarUser();
        Arena arena = criarArena();
        Quadra quadra = criarQuadra();
        LocalDate dataPassada = LocalDate.now().minusWeeks(1);

        when(quadraRepository.findById(quadra.getId())).thenReturn(Optional.of(quadra));

        assertThrows(IllegalArgumentException.class, () -> service.criarAssinaturaMensalista(
                user,
                arena,
                quadra.getId(),
                dataPassada.getDayOfWeek().getValue(),
                dataPassada,
                LocalTime.of(20, 0),
                LocalTime.of(21, 0)
        ));

        verify(asaasService, never()).createPaymentWithSplit(
                any(Double.class),
                any(Users.class),
                any(Arena.class),
                any(String.class),
                any(String.class)
        );
    }

    @Test
    void criarAssinaturaMensalistaRejeitaArenaSemWalletAntesDaCobranca() {
        Users user = criarUser();
        Arena arena = criarArena();
        arena.setAsaasWalletId(" ");
        Quadra quadra = criarQuadra();
        LocalDate primeiraData = proximaDataComMinimoDeOcorrencias(1);

        when(quadraRepository.findById(quadra.getId())).thenReturn(Optional.of(quadra));
        doThrow(new IllegalArgumentException("Arena nao configurada para receber split Asaas."))
                .when(asaasService).validarWalletSplitArena(" ");

        assertThrows(IllegalArgumentException.class, () -> service.criarAssinaturaMensalista(
                user,
                arena,
                quadra.getId(),
                primeiraData.getDayOfWeek().getValue(),
                primeiraData,
                LocalTime.of(20, 0),
                LocalTime.of(21, 0)
        ));

        verify(asaasService, never()).createPaymentWithSplit(
                any(Double.class),
                any(Users.class),
                any(Arena.class),
                any(String.class),
                any(String.class)
        );
    }

    @Test
    void criarAssinaturaMensalistaCancelaCobrancaAsaasQuandoFalhaAoSalvarAgendamento() {
        Users user = criarUser();
        Arena arena = criarArena();
        Quadra quadra = criarQuadra();
        LocalDate primeiraData = proximaDataComMinimoDeOcorrencias(1);

        ContratoMensalista contratoComId = new ContratoMensalista();
        contratoComId.setId(55);

        when(quadraRepository.findById(quadra.getId())).thenReturn(Optional.of(quadra));
        when(contratoRepository.save(any(ContratoMensalista.class)))
                .thenReturn(contratoComId)
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(agendamentoRepository.findAgendamentosDoDiaComSchema(
                eq(quadra.getId()),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                eq(arena.getSchemaName())
        )).thenReturn(Collections.emptyList());

        AsaasResponseDTO asaasResponse = new AsaasResponseDTO();
        asaasResponse.setId("pay_cancelar");
        asaasResponse.setInvoiceUrl("https://asaas.test/pay_cancelar");
        lenient().when(asaasService.createPaymentWithSplit(
                any(Double.class),
                eq(user),
                eq(arena),
                any(String.class),
                eq("MENSAL_55")
        )).thenReturn(asaasResponse);

        when(agendamentoRepository.salvarComSchema(any(Agendamentos.class), eq(arena.getSchemaName())))
                .thenThrow(new RuntimeException("Falha ao salvar agenda"));

        assertThrows(RuntimeException.class, () -> service.criarAssinaturaMensalista(
                user,
                arena,
                quadra.getId(),
                primeiraData.getDayOfWeek().getValue(),
                primeiraData,
                LocalTime.of(20, 0),
                LocalTime.of(21, 0)
        ));

        verify(asaasService).cancelarCobranca("pay_cancelar");
    }

    private Users criarUser() {
        Users user = new Users();
        user.setIdUser(7);
        user.setNome("Cliente Mensal");
        user.setEmail("cliente@email.com");
        user.setCpf("12345678901");
        user.setTelefone("41999999999");
        user.setAsaasCustomerId("cus_123");
        return user;
    }

    private Arena criarArena() {
        Arena arena = new Arena();
        arena.setId(3);
        arena.setName("Arena Teste");
        arena.setSchemaName("arena_teste");
        arena.setAsaasWalletId("wallet_123");
        arena.setDescontoMensalista(10.0);
        return arena;
    }

    private Quadra criarQuadra() {
        Quadra quadra = new Quadra();
        quadra.setId(11);
        quadra.setNome("Quadra 1");
        quadra.setValor_hora(100.0);
        return quadra;
    }

    private LocalDate proximaDataComMinimoDeOcorrencias(int minimo) {
        LocalDate data = LocalDate.now();
        while (contarOcorrenciasAteFimDoMes(data) < minimo) {
            data = data.plusDays(1);
        }
        return data;
    }

    private int contarOcorrenciasAteFimDoMes(LocalDate primeiraData) {
        int count = 0;
        LocalDate cursor = primeiraData;
        LocalDate fimDoMes = YearMonth.from(primeiraData).atEndOfMonth();
        DayOfWeek dia = primeiraData.getDayOfWeek();

        while (!cursor.isAfter(fimDoMes)) {
            if (cursor.getDayOfWeek() == dia) {
                count++;
            }
            cursor = cursor.plusDays(1);
        }
        return count;
    }
}
