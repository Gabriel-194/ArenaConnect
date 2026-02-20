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

    @Query("SELECT a.id FROM Arena a WHERE a.schemaName = :schema")
    Long findIdBySchemaName(@Param("schema") String schema);



    boolean existsByCnpj(String cnpj);

    Optional<Arena> findBySchemaName(String schemaName);

    @Query(value = """
    SELECT 
        a.id,           
        a.name,         
        a.endereco,     
        a.cidade,       
        a.estado,       
        a.latitude,     
        a.longitude,    
        (6371 * acos(
            cos(radians(:lat)) * cos(radians(a.latitude)) *
            cos(radians(a.longitude) - radians(:lon)) +
            sin(radians(:lat)) * sin(radians(a.latitude)))
        ) AS distance,  
        a.hora_inicio,  
        a.hora_fim      
    FROM public.arenas a 
    WHERE a.latitude IS NOT NULL 
    AND a.longitude IS NOT NULL 
    AND (:search IS NULL
        OR LOWER(a.name) LIKE LOWER(CONCAT('%', :search, '%'))
        OR LOWER(a.cidade) LIKE LOWER(CONCAT('%', :search, '%')))
    ORDER BY distance ASC
    LIMIT 9
""", nativeQuery = true)
    List<Object[]> findNearestWithDistance(
            @Param("lat") Double lat,
            @Param("lon") Double lon,
            @Param("search") String search
    );

    @Query(value = """
    SELECT 
        a.id,           
        a.name,         
        a.endereco,     
        a.cidade,       
        a.estado,       
        a.latitude,     
        a.longitude,    
        NULL as distance, 
        a.hora_inicio,
        a.hora_fim      
    FROM public.arenas a 
    WHERE (:search IS NULL 
           OR LOWER(a.name) LIKE LOWER(CONCAT('%', :search, '%')) 
           OR LOWER(a.cidade) LIKE LOWER(CONCAT('%', :search, '%')))
    ORDER BY a.id DESC 
    LIMIT 9
""", nativeQuery = true)
    List<Object[]> findRecent(@Param("search") String search);

    Optional<Arena> findByAssasSubscriptionId(String assasSubscriptionId);

}