package com.example.Utils;

import com.example.DTOs.ArenaDistanceDTO;

import java.util.ArrayList;
import java.util.List;

/**
 * Árvore Binária de Busca (BST) para busca eficiente de arenas por nome.
 *
 * A chave de ordenação é o nome da arena (case-insensitive).
 * Complexidade:
 *   - Inserção: O(log n) médio, O(n) pior caso
 *   - Busca por prefixo: O(log n + k) onde k = resultados encontrados
 *   - Travessia in-order: O(n)
 *
 * A árvore é reconstruída periodicamente pelo ArenaService para manter
 * os dados sincronizados com o banco.
 */
public class ArenaBST {

    private Node root;
    private int size;

    private static class Node {
        String key;  // nome da arena em lowercase para comparação
        ArenaDistanceDTO arena;
        Node left, right;

        Node(ArenaDistanceDTO arena) {
            this.key = arena.getName().toLowerCase();
            this.arena = arena;
        }
    }

    public ArenaBST() {
        this.root = null;
        this.size = 0;
    }

    /**
     * Insere uma arena na BST.
     * Ordenação pelo nome (case-insensitive).
     */
    public void insert(ArenaDistanceDTO arena) {
        if (arena == null || arena.getName() == null) return;
        root = insertRec(root, arena);
        size++;
    }

    private Node insertRec(Node node, ArenaDistanceDTO arena) {
        if (node == null) return new Node(arena);

        String key = arena.getName().toLowerCase();
        int cmp = key.compareTo(node.key);

        if (cmp < 0) {
            node.left = insertRec(node.left, key, arena);
        } else if (cmp > 0) {
            node.right = insertRec(node.right, key, arena);
        }
        // Se igual, não insere duplicata
        return node;
    }

    private Node insertRec(Node node, String key, ArenaDistanceDTO arena) {
        if (node == null) return new Node(arena);

        int cmp = key.compareTo(node.key);

        if (cmp < 0) {
            node.left = insertRec(node.left, key, arena);
        } else if (cmp > 0) {
            node.right = insertRec(node.right, key, arena);
        }
        return node;
    }

    /**
     * Busca arenas cujo nome contém o termo (case-insensitive).
     * Faz travessia in-order limitada a maxResults.
     *
     * Complexidade: O(n) no pior caso (precisa verificar todos os nós),
     * mas com early-exit quando maxResults é atingido.
     */
    public List<ArenaDistanceDTO> searchByName(String term, int maxResults) {
        List<ArenaDistanceDTO> results = new ArrayList<>();
        if (term == null || term.isBlank()) return results;

        String lowerTerm = term.toLowerCase().trim();
        searchInOrder(root, lowerTerm, results, maxResults);
        return results;
    }

    private void searchInOrder(Node node, String term, List<ArenaDistanceDTO> results, int maxResults) {
        if (node == null || results.size() >= maxResults) return;

        // Travessia in-order: esquerda → nó → direita
        searchInOrder(node.left, term, results, maxResults);

        if (results.size() < maxResults && node.key.contains(term)) {
            results.add(node.arena);
        }

        searchInOrder(node.right, term, results, maxResults);
    }

    /**
     * Busca arenas cujo nome começa com o prefixo dado.
     * Otimizada para poda de ramos desnecessários — O(log n + k).
     *
     * Esta é a busca mais eficiente pois aproveita a ordenação da BST
     * para podar subárvores que não podem conter o prefixo.
     */
    public List<ArenaDistanceDTO> searchByPrefix(String prefix, int maxResults) {
        List<ArenaDistanceDTO> results = new ArrayList<>();
        if (prefix == null || prefix.isBlank()) return results;

        String lowerPrefix = prefix.toLowerCase().trim();
        searchByPrefixRec(root, lowerPrefix, results, maxResults);
        return results;
    }

    private void searchByPrefixRec(Node node, String prefix, List<ArenaDistanceDTO> results, int maxResults) {
        if (node == null || results.size() >= maxResults) return;

        // Poda: se o prefixo é menor que a chave, pode haver match na esquerda
        if (prefix.compareTo(node.key) <= 0) {
            searchByPrefixRec(node.left, prefix, results, maxResults);
        }

        // Verifica o nó atual
        if (results.size() < maxResults && node.key.startsWith(prefix)) {
            results.add(node.arena);
        }

        // Poda: se o prefixo é maior ou igual, pode haver match na direita
        // Para prefixo, precisamos verificar se o próximo caractere após o prefixo
        // poderia existir na subárvore direita
        if (prefix.compareTo(node.key) >= 0 || node.key.startsWith(prefix)) {
            searchByPrefixRec(node.right, prefix, results, maxResults);
        }
    }

    /**
     * Retorna todas as arenas em ordem alfabética (in-order traversal).
     */
    public List<ArenaDistanceDTO> getAllSorted() {
        List<ArenaDistanceDTO> results = new ArrayList<>();
        inOrder(root, results);
        return results;
    }

    private void inOrder(Node node, List<ArenaDistanceDTO> results) {
        if (node == null) return;
        inOrder(node.left, results);
        results.add(node.arena);
        inOrder(node.right, results);
    }

    /**
     * Limpa a árvore para reconstrução.
     */
    public void clear() {
        root = null;
        size = 0;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return root == null;
    }

    /**
     * Constrói uma BST balanceada a partir de uma lista ordenada.
     * Garante O(log n) para todas as operações ao evitar degeneração.
     */
    public static ArenaBST buildBalanced(List<ArenaDistanceDTO> arenas) {
        ArenaBST bst = new ArenaBST();
        if (arenas == null || arenas.isEmpty()) return bst;

        // Ordena por nome para garantir inserção balanceada
        List<ArenaDistanceDTO> sorted = arenas.stream()
                .filter(a -> a.getName() != null)
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .toList();

        bst.root = buildBalancedRec(sorted, 0, sorted.size() - 1);
        bst.size = sorted.size();
        return bst;
    }

    private static Node buildBalancedRec(List<ArenaDistanceDTO> sorted, int start, int end) {
        if (start > end) return null;

        int mid = (start + end) / 2;
        Node node = new Node(sorted.get(mid));
        node.left = buildBalancedRec(sorted, start, mid - 1);
        node.right = buildBalancedRec(sorted, mid + 1, end);
        return node;
    }
}
