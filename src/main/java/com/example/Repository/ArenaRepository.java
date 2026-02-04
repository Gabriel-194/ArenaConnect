package com.example.Repository;

import com.example.Models.Arena;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArenaRepository extends JpaRepository<Arena, Long> {

    List<Arena> findByAtivoTrue();

    @Query("SELECT a.schemaName FROM Arena a WHERE a.id = :arenaId")
    String findSchemaNameById(@Param("arenaId") Long arenaId);

    boolean existsByCnpj(String cnpj);

    Optional<Arena> findBySchemaName(String schemaName);

    @Query(value = """
    SELECT 
        a.id,           -- row[0]
        a.name,         -- row[1]
        a.endereco,     -- row[2]
        a.cidade,       -- row[3]
        a.estado,       -- row[4]
        a.latitude,     -- row[5]
        a.longitude,    -- row[6]
        (6371 * acos(
            cos(radians(:lat)) * cos(radians(a.latitude)) *
            cos(radians(a.longitude) - radians(:lon)) +
            sin(radians(:lat)) * sin(radians(a.latitude)))
        ) AS distance,  -- row[7]
        a.schema_name   -- row[8]
    FROM public.arenas a 
    WHERE a.latitude IS NOT NULL 
    AND a.longitude IS NOT NULL 
    AND (:search IS NULL
        OR LOWER(a.name) LIKE LOWER(CONCAT('%', :search, '%'))
        OR LOWER(a.cidade) LIKE LOWER(CONCAT('%', :search, '%')))
    ORDER BY distance ASC
    LIMIT 10
""", nativeQuery = true)
    List<Object[]> findNearestWithDistance(
            @Param("lat") Double lat,
            @Param("lon") Double lon,
            @Param("search") String search
    );

    @Query(value = """
    SELECT 
        a.id,           -- row[0]
        a.name,         -- row[1]
        a.endereco,     -- row[2]
        a.cidade,       -- row[3]
        a.estado,       -- row[4]
        a.latitude,     -- row[5]
        a.longitude,    -- row[6]
        NULL as distance, -- row[7]
        a.schema_name   -- row[8]
    FROM public.arenas a 
    WHERE (:search IS NULL 
           OR LOWER(a.name) LIKE LOWER(CONCAT('%', :search, '%')) 
           OR LOWER(a.cidade) LIKE LOWER(CONCAT('%', :search, '%')))
    ORDER BY a.id DESC 
    LIMIT 10
""", nativeQuery = true)
    List<Object[]> findRecent(@Param("search") String search);
}