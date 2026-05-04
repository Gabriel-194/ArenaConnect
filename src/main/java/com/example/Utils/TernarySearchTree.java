package com.example.Utils;

import java.util.ArrayList;
import java.util.List;

public class TernarySearchTree<T> {
    private Node<T> root;

    private static class Node<T> {
        char data;
        Node<T> left, mid, right;
        List<T> values = new ArrayList<>();

        Node(char data) {
            this.data = data;
        }
    }

    public void insert(String key, T value) {
        if (key == null || key.isEmpty()) return;
        root = insert(root, key, value, 0);
    }

    private Node<T> insert(Node<T> node, String key, T value, int index) {
        char c = key.charAt(index);
        if (node == null) node = new Node<>(c);

        if (c < node.data) {
            node.left = insert(node.left, key, value, index);
        } else if (c > node.data) {
            node.right = insert(node.right, key, value, index);
        } else if (index < key.length() - 1) {
            node.mid = insert(node.mid, key, value, index + 1);
        } else {
            node.values.add(value);
        }
        return node;
    }

    public List<T> search(String key) {
        Node<T> node = search(root, key, 0);
        return (node == null) ? new ArrayList<>() : node.values;
    }

    private Node<T> search(Node<T> node, String key, int index) {
        if (node == null) return null;
        char c = key.charAt(index);

        if (c < node.data) {
            return search(node.left, key, index);
        } else if (c > node.data) {
            return search(node.right, key, index);
        } else if (index < key.length() - 1) {
            return search(node.mid, key, index + 1);
        } else {
            return node;
        }
    }

    public List<T> searchByPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) return new ArrayList<>();

        List<T> results = new ArrayList<>();
        Node<T> node = search(root, prefix, 0);
        if (node == null) return results;

        // Limitar a coleta para evitar retornos massivos com prefixos curtos
        results.addAll(node.values);
        collect(node.mid, results, 500); // Limite máximo de resultados
        return results;
    }

    /**
     * Coleta com limite máximo de resultados para evitar O(n) com prefixos curtos.
     * Antes: collect sem limite poderia retornar toda a árvore.
     */
    private void collect(Node<T> node, List<T> results, int maxResults) {
        if (node == null || results.size() >= maxResults) return;
        collect(node.left, results, maxResults);
        if (results.size() >= maxResults) return;
        results.addAll(node.values);
        collect(node.mid, results, maxResults);
        if (results.size() >= maxResults) return;
        collect(node.right, results, maxResults);
    }
}
