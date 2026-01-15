package com.example.Repository;

import com.example.Models.Arena;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArenaRepository extends JpaRepository<Arena, Integer> {

    Optional<Arena> findByCnpj(String cnpj);
    boolean existsByCnpj(String cnpj);
    Optional<Arena> findBySchemaName(String schemaName);
    boolean existsBySchemaName(String schemaName);
    List<Arena> findByAtivoTrue();
    List<Arena> findByCidade(String cidade);
    List<Arena> findByCidadeAndAtivoTrue(String cidade);
    List<Arena> findByEstado(String estado);
    List<Arena> findByNameContainingIgnoreCase(String name);
}