package com.example.Repository;

import com.example.Models.Arena;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArenaRepository extends JpaRepository<Arena, Long> {

    List<Arena> findByAtivoTrue();


    @Query("SELECT a.schemaName FROM Arena a WHERE a.id = :arenaId")
    String findSchemaNameById(Long arenaId);

    boolean existsByCnpj(String cnpj);
}