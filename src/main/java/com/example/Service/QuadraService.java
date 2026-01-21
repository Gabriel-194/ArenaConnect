package com.example.Service;

import com.example.Models.Quadra;
import com.example.Multitenancy.TenantContext;
import com.example.Repository.QuadraRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class QuadraService {

    private static final Logger logger = LoggerFactory.getLogger(QuadraService.class);

    @Autowired
    private QuadraRepository quadraRepository;


    private String configurarSchema() {
        String currentTenant = TenantContext.getCurrentTenant();

        if (currentTenant != null && !currentTenant.isEmpty()) {
            return currentTenant;
        } else {
            return "public";
        }
    }

    @Transactional
    public Quadra cadastrar(Quadra quadra) {
        if (quadra == null) {
            logger.error("❌ Tentativa de cadastrar quadra nula");
            throw new IllegalArgumentException("Dados da Quadra não podem estar vazios");
        }

        if (quadra.getNome().isEmpty()) {
            throw new IllegalArgumentException("Nome da quadra deve ser informada");

        } else if (quadra.getTipo_quadra().isEmpty()) {
            throw new IllegalArgumentException("Tipo de quadra deve ser informada");

        } else if (quadra.getValor_hora() == null) {
            throw new IllegalArgumentException("Valor da quadra deve ser informada");

        }

        quadra.setAtivo(true);
        return quadraRepository.salvarComSchema(quadra, configurarSchema());
    }

    @Transactional
    public List<Quadra> getAll() {
        return quadraRepository.listarTodasComSchema(configurarSchema());
    }

    @Transactional
    public void alterarStatus(Integer id) {
        if (id == null) throw new RuntimeException("ID inválido");

        quadraRepository.alterarStatusComSchema(id, configurarSchema());
    }

    public List<Quadra> findByAtivoTrue() {
        return quadraRepository.findByAtivoTrue();
    }


    @Transactional
    public Quadra atualizar (Quadra quadraAtualizada) {
        if(quadraAtualizada.getId() == null) throw new IllegalArgumentException("ID obrigatório para atualização");

        return quadraRepository.salvarComSchema(quadraAtualizada, configurarSchema());
    }

    @Transactional
    public Optional<Quadra> buscarPorId(Integer id) {
        return quadraRepository.buscarPorIdComSchema(id, configurarSchema());
    }


}
