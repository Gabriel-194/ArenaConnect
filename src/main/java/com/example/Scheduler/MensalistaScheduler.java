package com.example.Scheduler;

import com.example.DTOs.Asaas.AsaasResponseDTO;
import com.example.Models.Arena;
import com.example.Models.ContratoMensalista;
import com.example.Models.Quadra;
import com.example.Models.Users;
import com.example.Repository.ArenaRepository;
import com.example.Repository.ContratoMensalistaRepository;
import com.example.Repository.QuadraRepository;
import com.example.Repository.UserRepository;
import com.example.Service.AsaasService;
import com.example.Multitenancy.TenantContext;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Component
public class MensalistaScheduler {

    @Autowired private ArenaRepository arenaRepository;
    @Autowired private ContratoMensalistaRepository contratoRepository;
    @Autowired private QuadraRepository quadraRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AsaasService asaasService;

    // Roda todo dia 20 de cada mês às 03:00 da madrugada
    @Scheduled(cron = "0 0 3 20 * ?")
    @Transactional
    public void gerarCobrancasDoProximoMes() {
        System.out.println("🤖 SCHEDULER: Iniciando geração de mensalidades para o próximo mês...");

        List<Arena> arenas = arenaRepository.findAll();
        LocalDate mesQueVem = LocalDate.now().plusMonths(1);

        for (Arena arena : arenas) {
            if (!arena.isAtivo() || arena.getSchemaName() == null || "public".equals(arena.getSchemaName())) continue;

            try {
                TenantContext.setCurrentTenant(arena.getSchemaName());

                List<ContratoMensalista> contratosAtivos = contratoRepository.findByAtivoTrue();

                for (ContratoMensalista contrato : contratosAtivos) {
                    Users user = userRepository.findById(contrato.getIdUser()).orElse(null);
                    Quadra quadra = quadraRepository.findById(contrato.getIdQuadra()).orElse(null);

                    if (user == null || quadra == null) continue;

                    // 1. Calcula quantas vezes o dia cai no MÊS QUE VEM
                    int quantidadeJogosMesQueVem = calcularDiasNoMes(mesQueVem, DayOfWeek.of(contrato.getDiaSemana()));

                    // 2. Calcula o valor
                    double valorBruto = quantidadeJogosMesQueVem * quadra.getValor_hora();
                    double percentualDesconto = (arena.getDescontoMensalista() != null) ? arena.getDescontoMensalista() : 0.0;
                    double valorComDesconto = valorBruto - (valorBruto * (percentualDesconto / 100.0));

                    // 3. Atualiza o Contrato para PENDENTE com o novo valor
                    contrato.setValorPactuado(valorComDesconto);
                    contrato.setStatus("PENDENTE");

                    // 4. Cria a cobrança no Asaas
                    String externalReference = "MENSAL_" + contrato.getId();
                    String descricao = "Renovação Mensalidade (" + mesQueVem.getMonth() + ") - " + quantidadeJogosMesQueVem + " jogos.";

                    AsaasResponseDTO asaasResp = asaasService.createPaymentWithSplit(
                            valorComDesconto, user, arena, descricao, externalReference
                    );

                    contrato.setAsaasPaymentId(asaasResp.getId());
                    contrato.setAsaasInvoiceUrl(asaasResp.getInvoiceUrl());
                    contratoRepository.save(contrato);

                    System.out.println("✅ Fatura gerada: Contrato " + contrato.getId() + " - Arena: " + arena.getName());
                }
            } catch (Exception e) {
                System.err.println("Erro ao processar mensalidades da arena " + arena.getName() + ": " + e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
    }

    // Helper para contar quantos dias específicos (ex: 4 sextas) existem num mês
    private int calcularDiasNoMes(LocalDate dataReferencia, DayOfWeek diaEscolhido) {
        LocalDate inicioMes = YearMonth.from(dataReferencia).atDay(1);
        LocalDate fimMes = YearMonth.from(dataReferencia).atEndOfMonth();

        int count = 0;
        LocalDate iterador = inicioMes;

        while (!iterador.isAfter(fimMes)) {
            if (iterador.getDayOfWeek() == diaEscolhido) {
                count++;
            }
            iterador = iterador.plusDays(1);
        }
        return count;
    }
}