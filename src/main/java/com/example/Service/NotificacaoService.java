package com.example.Service; // Ajuste o pacote

import com.example.Models.Notificacao;
import com.example.Repository.NotificacaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificacaoService {

    @Autowired
    private NotificacaoRepository notificacaoRepository;

    public void enviar(Long userId, String titulo, String mensagem, String tipo) {
        // Evita erro caso seja um cliente avulso (sem user_id logado)
        if (userId != null) {
            Notificacao novaNotificacao = new Notificacao(userId, titulo, mensagem, tipo);
            notificacaoRepository.save(novaNotificacao);
        }
    }

    public List<Notificacao> buscarMinhasNotificacoes(Long userId) {
        return notificacaoRepository.findByUserIdOrderByDataCriacaoDesc(userId);
    }

    public void marcarComoLidas(Long userId) {
        List<Notificacao> pendentes = notificacaoRepository.findByUserIdOrderByDataCriacaoDesc(userId)
                .stream().filter(n -> !n.getLida()).toList();

        for (Notificacao n : pendentes) {
            n.setLida(true);
        }
        notificacaoRepository.saveAll(pendentes);
    }
}