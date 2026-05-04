/**
 * 🌳 ArenaBST — Árvore Binária de Busca para filtragem local de arenas.
 *
 * Permite busca O(log n + k) no frontend, eliminando a latência
 * de rede para resultados de pesquisa enquanto o debounce aguarda.
 */

class BSTNode {
    constructor(arena) {
        this.key = (arena.name || arena.nome || '').toLowerCase();
        this.arena = arena;
        this.left = null;
        this.right = null;
    }
}

export class ArenaBST {
    constructor() {
        this.root = null;
        this.size = 0;
    }

    /**
     * Insere uma arena na BST ordenada pelo nome.
     */
    insert(arena) {
        if (!arena || !(arena.name || arena.nome)) return;
        this.root = this._insertRec(this.root, arena);
        this.size++;
    }

    _insertRec(node, arena) {
        if (!node) return new BSTNode(arena);

        const key = (arena.name || arena.nome || '').toLowerCase();
        if (key < node.key) {
            node.left = this._insertRec(node.left, arena);
        } else if (key > node.key) {
            node.right = this._insertRec(node.right, arena);
        }
        return node;
    }

    /**
     * Busca arenas cujo nome contém o termo (case-insensitive).
     * Retorna até maxResults resultados.
     */
    searchByName(term, maxResults = 12) {
        const results = [];
        if (!term || !term.trim()) return results;

        const lowerTerm = term.toLowerCase().trim();
        this._searchInOrder(this.root, lowerTerm, results, maxResults);
        return results;
    }

    _searchInOrder(node, term, results, maxResults) {
        if (!node || results.length >= maxResults) return;

        this._searchInOrder(node.left, term, results, maxResults);

        if (results.length < maxResults) {
            // Busca tanto no nome quanto na cidade
            const cidade = (node.arena.cidade || '').toLowerCase();
            if (node.key.includes(term) || cidade.includes(term)) {
                results.push(node.arena);
            }
        }

        this._searchInOrder(node.right, term, results, maxResults);
    }

    /**
     * Limpa a árvore.
     */
    clear() {
        this.root = null;
        this.size = 0;
    }

    /**
     * Constrói uma BST balanceada a partir de uma lista de arenas.
     * Garante altura O(log n) para evitar degeneração.
     */
    static buildBalanced(arenas) {
        const bst = new ArenaBST();
        if (!arenas || arenas.length === 0) return bst;

        // Ordena por nome para inserção balanceada
        const sorted = [...arenas]
            .filter(a => a.name || a.nome)
            .sort((a, b) => {
                const nameA = (a.name || a.nome || '').toLowerCase();
                const nameB = (b.name || b.nome || '').toLowerCase();
                return nameA.localeCompare(nameB);
            });

        bst.root = ArenaBST._buildBalancedRec(sorted, 0, sorted.length - 1);
        bst.size = sorted.length;
        return bst;
    }

    static _buildBalancedRec(sorted, start, end) {
        if (start > end) return null;

        const mid = Math.floor((start + end) / 2);
        const node = new BSTNode(sorted[mid]);
        node.left = ArenaBST._buildBalancedRec(sorted, start, mid - 1);
        node.right = ArenaBST._buildBalancedRec(sorted, mid + 1, end);
        return node;
    }
}
