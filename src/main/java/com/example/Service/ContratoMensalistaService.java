package com.example.Service;

import com.example.DTOs.Asaas.AsaasResponseDTO;
import com.example.Models.*;
import com.example.Repository.ArenaRepository;
import com.example.Repository.ContratoMensalistaRepository;
import com.example.Repository.QuadraRepository;
import com.example.Models.ContratoMensalista;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

    @Transactional
    public ContratoMensalista criarAssinaturaMensalista(Users user, Arena arena, Integer idQuadra, int diaSemanaNumero, LocalTime horaInicio, LocalTime horaFim) {

        Quadra quadra = quadraRepository.findById(idQuadra)
                .orElseThrow(() -> new RuntimeException("Quadra não encontrada"));

        DayOfWeek diaEscolhido = DayOfWeek.of(diaSemanaNumero); // 1 = Segunda, 7 = Domingo

        // 1. Calcula os dias restantes neste mês
        LocalDate hoje = LocalDate.now();
        LocalDate fimDoMes = YearMonth.from(hoje).atEndOfMonth();

        int quantidadeJogosRestantes = 0;
        LocalDate dataIteracao = hoje;

        while (!dataIteracao.isAfter(fimDoMes)) {
            if (dataIteracao.getDayOfWeek() == diaEscolhido) {
                quantidadeJogosRestantes++;
            }
            dataIteracao = dataIteracao.plusDays(1);
        }

        if (quantidadeJogosRestantes == 0) {
            throw new IllegalArgumentException("Não há mais dias disponíveis para este dia da semana neste mês. Aguarde o mês seguinte.");
        }

        // 2. Calcula os valores (com desconto)
        double valorBruto = quantidadeJogosRestantes * quadra.getValor_hora();
        // Atenção: Certifique-se de que a entidade Arena possui o atributo `descontoMensalista`
        double percentualDesconto = (arena.getDescontoMensalista() != null) ? arena.getDescontoMensalista() : 0.0;
        double valorComDesconto = valorBruto - (valorBruto * (percentualDesconto / 100.0));

        // 3. Salva o Contrato no Banco
        ContratoMensalista contrato = new ContratoMensalista();
        contrato.setIdUser(user.getIdUser());
        contrato.setIdQuadra(idQuadra);
        contrato.setDiaSemana(diaSemanaNumero);
        contrato.setHoraInicio(horaInicio);
        contrato.setHoraFim(horaFim);
        contrato.setValorPactuado(valorComDesconto);
        contrato.setStatus("PENDENTE");
        contrato.setAtivo(true);

        contrato = contratoRepository.save(contrato);

        // 4. Integração com o Asaas
        String externalReference = "MENSAL_" + contrato.getId();
        String descricao = "Mensalidade ArenaConnect - " + quantidadeJogosRestantes + " jogos restantes para este mês.";

        AsaasResponseDTO asaasResponse = asaasService.createPaymentWithSplit(
                valorComDesconto, user, arena, descricao, externalReference
        );

        // 5. Atualiza o contrato com os dados do Asaas
        contrato.setAsaasPaymentId(asaasResponse.getId());
        contrato.setAsaasInvoiceUrl(asaasResponse.getInvoiceUrl());

        return contratoRepository.save(contrato);
    }

    public List<ContratoMensalista> listarMeusContratos(Users user) {
        List<Arena> arenas = arenaRepository.findAll();

        // Usa LinkedHashSet para deduplicar contratos (equals/hashCode consideram id + idArena)
        Set<ContratoMensalista> contratosUnicos = new LinkedHashSet<>();

        for (Arena arena : arenas) {
            if (!arena.isAtivo() || arena.getSchemaName() == null || "public".equals(arena.getSchemaName())) {
                continue;
            }

            try {
                // Usa o método custom que faz SET search_path TO antes da query
                List<ContratoMensalista> contratosArena = contratoRepository.findByIdUserComSchema(
                        user.getIdUser(), arena.getSchemaName()
                );

                for (ContratoMensalista contrato : contratosArena) {
                    // Preenche os campos transientes para o frontend saber de qual arena vem
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
