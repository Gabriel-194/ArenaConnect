package com.example.Repository.Custom;

import com.example.Models.ContratoMensalista;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ContratoMensalistaRepositoryImpl implements ContratoMensalistaRepositoryCustom {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public List<ContratoMensalista> findByIdUserComSchema(Integer idUser, String schema) {
        if (schema == null || schema.isEmpty()) schema = "public";

        // 🔧 Segurança: Valida que o schema contém apenas caracteres alfanuméricos e underscore
        if (!schema.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("Nome de schema inválido: " + schema);
        }

        // Define o schema antes da query (mesmo padrão do AgendamentoRepositoryImpl)
        jdbcTemplate.execute("SET search_path TO " + schema);

        String sql = "SELECT id, id_user, id_quadra, dia_semana, hora_inicio, hora_fim, " +
                     "valor_pactuado, status, ativo, asaas_payment_id, asaas_invoice_url " +
                     "FROM contratos_mensalistas WHERE id_user = ?";

        return jdbcTemplate.query(sql, new Object[]{idUser}, (rs, rowNum) -> {
            ContratoMensalista c = new ContratoMensalista();
            c.setId(rs.getInt("id"));
            c.setIdUser(rs.getInt("id_user"));
            c.setIdQuadra(rs.getInt("id_quadra"));
            c.setDiaSemana(rs.getInt("dia_semana"));

            Time horaInicio = rs.getTime("hora_inicio");
            if (horaInicio != null) c.setHoraInicio(horaInicio.toLocalTime());

            Time horaFim = rs.getTime("hora_fim");
            if (horaFim != null) c.setHoraFim(horaFim.toLocalTime());

            c.setValorPactuado(rs.getDouble("valor_pactuado"));
            c.setStatus(rs.getString("status"));
            c.setAtivo(rs.getBoolean("ativo"));
            c.setAsaasPaymentId(rs.getString("asaas_payment_id"));
            c.setAsaasInvoiceUrl(rs.getString("asaas_invoice_url"));
            return c;
        });
    }
}
