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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class MensalistaScheduler {

    private static final Logger logger = LoggerFactory.getLogger(MensalistaScheduler.class);

    @Autowired private ArenaRepository arenaRepository;
    @Autowired private ContratoMensalistaRepository contratoRepository;
    @Autowired private QuadraRepository quadraRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AsaasService asaasService;

    // Roda todo dia 20 de cada mês às 03:00 da madrugada
    @Scheduled(cron = "0 0 3 20 * ?")
    @Transactional
    public void gerarCobrancasDoProximoMes() {
        logger.info("🤖 SCHEDULER: Iniciando geração de mensalidades para o próximo mês...");

        // 🔧 Otimização: Busca apenas arenas ativas ao invés de findAll()
        List<Arena> arenas = arenaRepository.findByAtivoTrue();
        LocalDate mesQueVem = LocalDate.now().plusMonths(1);

        for (Arena arena : arenas) {
            if (arena.getSchemaName() == null || "public".equals(arena.getSchemaName())) continue;

            try {
                TenantContext.setCurrentTenant(arena.getSchemaName());

                List<ContratoMensalista> contratosAtivos = contratoRepository.findByAtivoTrue();

                if (contratosAtivos.isEmpty()) continue;

                // 🔧 Otimização N+1: Batch fetch de users e quadras em 2 queries
                // Antes: 2 queries por contrato (user + quadra) = O(C × 2)
                // Agora: 2 queries totais = O(1)
                Set<Integer> userIds = contratosAtivos.stream()
                        .map(ContratoMensalista::getIdUser)
                        .collect(Collectors.toSet());

                Set<Integer> quadraIds = contratosAtivos.stream()
                        .map(ContratoMensalista::getIdQuadra)
                        .collect(Collectors.toSet());

                Map<Integer, Users> usersMap = userRepository.findAllById(userIds).stream()
                        .collect(Collectors.toMap(Users::getIdUser, Function.identity()));

                Map<Integer, Quadra> quadrasMap = quadraRepository.findAllById(quadraIds).stream()
                        .collect(Collectors.toMap(Quadra::getId, Function.identity()));

                for (ContratoMensalista contrato : contratosAtivos) {
                    Users user = usersMap.get(contrato.getIdUser());
                    Quadra quadra = quadrasMap.get(contrato.getIdQuadra());

                    if (user == null || quadra == null) continue;

                    try {
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

                        logger.info("✅ Fatura gerada: Contrato {} - Arena: {}", contrato.getId(), arena.getName());
                    } catch (Exception e) {
                        // 🔧 Isolamento de erro por contrato: uma falha não bloqueia os demais
                        logger.error("❌ Erro ao gerar fatura do contrato {} na arena {}: {}",
                                contrato.getId(), arena.getName(), e.getMessage());
                    }
                }
            } catch (Exception e) {
                logger.error("Erro ao processar mensalidades da arena {}: {}", arena.getName(), e.getMessage());
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