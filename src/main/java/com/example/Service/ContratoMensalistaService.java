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

    public java.util.List<ContratoMensalista> listarMeusContratos(Users user) {
        java.util.List<ContratoMensalista> todosContratos = new java.util.ArrayList<>();

        // Pega todas as arenas para iterar pelos schemas
        java.util.List<Arena> arenas = arenaRepository.findAll();

        for (Arena arena : arenas) {
            if (!arena.isAtivo() || arena.getSchemaName() == null || "public".equals(arena.getSchemaName())) {
                continue;
            }

            try {
                // Muda para o schema da arena atual
                com.example.Multitenancy.TenantContext.setCurrentTenant(arena.getSchemaName());

                // Busca os contratos do usuário nesta arena
                java.util.List<ContratoMensalista> contratosArena = contratoRepository.findByIdUser(user.getIdUser());

                todosContratos.addAll(contratosArena);
            } catch (Exception e) {
                // Se der erro (ex: tabela ainda não existe nesta arena), apenas ignora e vai para a próxima
                System.err.println("Aviso: Não foi possível buscar contratos na arena " + arena.getName() + " - " + e.getMessage());
            } finally {
                // Limpa o contexto para a próxima iteração
                com.example.Multitenancy.TenantContext.clear();
            }
        }

        return todosContratos;
    }
}