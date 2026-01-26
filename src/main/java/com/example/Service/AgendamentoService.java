package com.example.Service;

import com.example.Models.Agendamentos;
import com.example.Multitenancy.TenantContext;
import com.example.Repository.AgendamentoRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AgendamentoService {

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    private String configurarSchema() {
        String currentTenant = TenantContext.getCurrentTenant();

        if (currentTenant != null && !currentTenant.isEmpty()) {
            return currentTenant;
        } else {
            return "public";
        }
    }

    @Transactional
    public List<LocalTime> getHorariosDisponiveis(Integer idQuadra, LocalDate data) {
        String schema = configurarSchema();

        LocalTime abertura = LocalTime.of(7, 30);
        LocalTime fechamento = LocalTime.of(23, 30);

        LocalDateTime inicioDia = data.atStartOfDay();
        LocalDateTime fimDia = data.plusDays(1).atStartOfDay();

        List<Agendamentos> agendamentos = agendamentoRepository.findAgendamentosDoDiaComSchema(idQuadra, inicioDia, fimDia, schema);

        List<LocalTime> horariosOcupados = agendamentos.stream()
                .map(a -> a.getData_inicio().toLocalTime())
                .collect(Collectors.toList());

        List<LocalTime> horariosDisponiveis = new ArrayList<>();
        LocalTime atual = abertura;

        while(atual.isBefore(fechamento)) {
            if(!horariosOcupados.contains(atual)) {
                horariosDisponiveis.add(atual);
            }
            atual = atual.plusHours(1);
        }
        return horariosDisponiveis;
    }

    @Transactional
    public Agendamentos createBooking(Agendamentos newBooking){
        if (newBooking.getId_quadra() == null) {
            throw new IllegalArgumentException("ID da quadra não pode ser nulo");
        }

        if (newBooking.getData_inicio() == null) {
            throw new IllegalArgumentException("Data de início não pode ser nula");
        }

        LocalDate data = newBooking.getData_inicio().toLocalDate();
        LocalTime horario = newBooking.getData_inicio().toLocalTime();

        List<LocalTime> horariosDisponiveis = getHorariosDisponiveis(newBooking.getId_quadra(), data);

        if (!horariosDisponiveis.contains(horario)) {
            throw new IllegalArgumentException("Horário não está disponível para agendamento");
        }

        if (newBooking.getData_inicio() != null) {
            newBooking.setData_fim(newBooking.getData_inicio().plusHours(1));
        }

        newBooking.setStatus("CONFIRMADO");
        return agendamentoRepository.salvarComSchema(newBooking, configurarSchema());
    }
}
