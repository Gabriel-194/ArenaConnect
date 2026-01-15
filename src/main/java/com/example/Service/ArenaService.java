package com.example.Service;

import com.example.Models.Arena;
import com.example.Repository.ArenaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ArenaService {

    @Autowired
    private ArenaRepository arenaRepository;


    public boolean cnpjExists(String cnpj) {
        return arenaRepository.existsByCnpj(cnpj);
    }


    public Optional<Arena> findById(Integer id) {
        return arenaRepository.findById(id);
    }


    public Optional<Arena> findByCnpj(String cnpj) {
        return arenaRepository.findByCnpj(cnpj);
    }


    public List<Arena> findAllActive() {
        return arenaRepository.findByAtivoTrue();
    }


    public List<Arena> findAll() {
        return arenaRepository.findAll();
    }


    public Optional<Arena> findBySchemaName(String schemaName) {
        return arenaRepository.findBySchemaName(schemaName);
    }

    @Transactional
    public Arena save(Arena arena) {
        return arenaRepository.save(arena);
    }

    @Transactional
    public void deactivate(Integer id) {
        Arena arena = arenaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Arena não encontrada"));
        arena.setAtivo(false);
        arenaRepository.save(arena);
    }

    @Transactional
    public void activate(Integer id) {
        Arena arena = arenaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Arena não encontrada"));
        arena.setAtivo(true);
        arenaRepository.save(arena);
    }
}