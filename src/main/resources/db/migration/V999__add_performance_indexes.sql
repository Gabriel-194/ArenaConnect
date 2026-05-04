-- =====================================================
-- ArenaConnect: Índices de Performance
-- Execute este script em cada schema de tenant
-- e no schema public para a tabela de histórico.
-- =====================================================

-- 🔧 Histórico: Acelera busca por status (usado por 3 schedulers)
-- Impacta: PaymentReconciliationScheduler, AgendamentoScheduler
CREATE INDEX IF NOT EXISTS idx_historico_status_data
    ON public.agendamentos_historico(status, data_inicio);

-- 🔧 Histórico: Acelera webhook de pagamento (busca por payment_id)
-- Impacta: confirmPaymentWebhook, reconciliarPagamentosPendentes
CREATE INDEX IF NOT EXISTS idx_historico_payment
    ON public.agendamentos_historico(asaas_payment_id);

-- 🔧 Histórico: Acelera busca por usuário (usado em findAgendamentosClients)
CREATE INDEX IF NOT EXISTS idx_historico_user
    ON public.agendamentos_historico(id_user);

-- 🔧 Histórico: Acelera busca por origem (idAgendamento + idArena)
CREATE INDEX IF NOT EXISTS idx_historico_origem
    ON public.agendamentos_historico(id_agendamento, id_arena);

-- =====================================================
-- ÍNDICES POR TENANT (executar em cada schema)
-- Substitua {schema} pelo nome do schema do tenant
-- =====================================================

-- 🔧 Agendamentos: Acelera busca por quadra e data (usado em quase toda query)
-- Impacta: getHorariosDisponiveis, findAllAgendamentos, validarDisponibilidade
-- CREATE INDEX IF NOT EXISTS idx_agendamentos_quadra_data
--     ON {schema}.agendamentos(id_quadra, data_inicio);

-- 🔧 Agendamentos: Acelera filtro por status
-- Impacta: finalizarAgendamentosPorIds, cancelarAgendamentosPorIds
-- CREATE INDEX IF NOT EXISTS idx_agendamentos_status
--     ON {schema}.agendamentos(status);

-- 🔧 Contratos: Acelera busca por usuário e ativo
-- Impacta: listarMeusContratos, gerarCobrancasDoProximoMes
-- CREATE INDEX IF NOT EXISTS idx_contratos_user_ativo
--     ON {schema}.contratos_mensalistas(id_user, ativo);
