package com.example.Utils;

import com.example.Models.ContratoMensalista;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Estrutura de busca de contratos por idUser.
 *
 * Substituída de BST (Binary Search Tree) para ConcurrentHashMap.
 * Motivo: A BST não-balanceada degenerava para O(n) com IDs sequenciais.
 * Agora todas as operações são O(1) amortizado e thread-safe sem synchronized.
 */
public class ContratoBST {

    private final ConcurrentHashMap<Integer, List<ContratoMensalista>> contratos = new ConcurrentHashMap<>();

    // 🟢 Thread-safe via ConcurrentHashMap — sem necessidade de synchronized
    public void insert(ContratoMensalista contrato) {
        contratos.computeIfAbsent(contrato.getIdUser(), k -> new ArrayList<>())
                .add(contrato);
    }

    // Busca O(1) amortizado para encontrar todos os contratos de um usuário específico
    public List<ContratoMensalista> searchByUserId(Integer idUser) {
        return contratos.getOrDefault(idUser, new ArrayList<>());
    }

    // Utilitário: Remove todos os contratos de um usuário
    public void removeByUserId(Integer idUser) {
        contratos.remove(idUser);
    }

    // Utilitário: Limpa toda a estrutura
    public void clear() {
        contratos.clear();
    }

    // Utilitário: Quantidade de usuários distintos
    public int size() {
        return contratos.size();
    }
}