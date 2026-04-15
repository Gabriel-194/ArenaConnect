package com.example.Utils;

import com.example.Models.ContratoMensalista;
import java.util.ArrayList;
import java.util.List;

public class ContratoBST {

    // Nó da Árvore
    private static class Node {
        Integer idUser;
        // Uma lista porque um mesmo usuário pode ter vários contratos (mesmo id_user)
        List<ContratoMensalista> contratos;
        Node left, right;

        Node(ContratoMensalista contrato) {
            this.idUser = contrato.getIdUser();
            this.contratos = new ArrayList<>();
            this.contratos.add(contrato);
            this.left = this.right = null;
        }
    }

    private Node root;

    // 🟢 O bloco 'synchronized' garante que a Busca Paralela não corrompe a árvore
    public synchronized void insert(ContratoMensalista contrato) {
        root = insertRec(root, contrato);
    }

    private Node insertRec(Node root, ContratoMensalista contrato) {
        if (root == null) {
            return new Node(contrato);
        }

        // Se o id_user já existe no nó, apenas adiciona o contrato à lista daquele usuário
        if (contrato.getIdUser().equals(root.idUser)) {
            root.contratos.add(contrato);
        }
        // Se o id_user for menor, vai para a subárvore esquerda
        else if (contrato.getIdUser() < root.idUser) {
            root.left = insertRec(root.left, contrato);
        }
        // Se o id_user for maior, vai para a subárvore direita
        else {
            root.right = insertRec(root.right, contrato);
        }

        return root;
    }

    // Busca O(log n) para encontrar todos os contratos de um usuário específico
    public List<ContratoMensalista> searchByUserId(Integer idUser) {
        Node resultNode = searchRec(root, idUser);
        if (resultNode != null) {
            return resultNode.contratos;
        }
        return new ArrayList<>(); // Retorna lista vazia se não encontrar
    }

    private Node searchRec(Node root, Integer idUser) {
        // Base case: raiz é nula ou a chave está na raiz
        if (root == null || root.idUser.equals(idUser)) {
            return root;
        }

        // O valor procurado é menor que a chave da raiz
        if (root.idUser > idUser) {
            return searchRec(root.left, idUser);
        }

        // O valor procurado é maior que a chave da raiz
        return searchRec(root.right, idUser);
    }
}