package com.example.Service;

import com.example.DTOs.PartnerRegistrationDTO;
import com.example.Models.Arena;
import com.example.Repository.ArenaRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ArenaService {

    @Autowired
    private ArenaRepository arenaRepository;

    private static final Logger logger = LoggerFactory.getLogger(ArenaService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Transactional
    public Arena cadastrarArena(Arena arena){


        String schemaName = arena.getName()
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");

        arena.setSchemaName(schemaName);

        Arena savedArena = arenaRepository.save(arena);
        logger.info("✅ Arena salva no banco: ID={}, Nome={}", savedArena.getId(), savedArena.getName());

        try {
            String createSchemaSQL = "CREATE SCHEMA IF NOT EXISTS \"" + schemaName + "\"";
            jdbcTemplate.execute(createSchemaSQL);
            logger.info("✅ Schema criado: {}", schemaName);
        } catch (Exception e) {
            logger.error("❌ Erro ao criar schema: {}", e.getMessage());
            throw new RuntimeException("Falha ao criar schema", e);
        }

        try {
            createSchemaTablesFromFile(schemaName);
            logger.info("✅ Tabelas criadas com sucesso no schema: {}", schemaName);
        } catch (Exception e) {
            logger.error("❌ Erro ao criar tabelas: {}", e.getMessage(), e);
            throw new RuntimeException("Falha ao criar tabelas no schema", e);
        }

        return savedArena;
    }

    @Transactional
    public void createSchemaTablesFromFile(String schemaName) {
        try {
            ClassPathResource resource = new ClassPathResource("sql/schema-template.sql");

            if (!resource.exists()) {
                throw new RuntimeException("Arquivo schema-template.sql não encontrado em src/main/resources/sql/");
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

                String sqlTemplate = reader.lines()
                        .filter(line -> !line.trim().startsWith("--") && !line.trim().isEmpty())
                        .collect(Collectors.joining("\n"));

                String sql = sqlTemplate.replace("{SCHEMA_NAME}", schemaName);


                jdbcTemplate.execute((ConnectionCallback<Object>) con -> {
                    ScriptUtils.executeSqlScript(
                            con,
                            new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8))
                    );
                    return null;
                });
            }

        } catch (Exception e) {
            logger.error("❌ Erro fatal ao criar tabelas no schema {}: {}", schemaName, e.getMessage());
            throw new RuntimeException("Falha ao criar estrutura do schema", e);
        }
    }

    public List<Arena> listarArenas() {
        logger.info("Listando todas as arenas");
        return arenaRepository.findByAtivoTrue();
    }

    public Arena buscarArenaPorId(long id) {
        logger.info("Buscando arena por ID: {}", id);
        return arenaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Arena não encontrada para o Id ID:" + id));
    }

    public List<Arena> buscarArenasPorNome(String name) {
        logger.info("Buscando arenas por nome: {}", name);
        return arenaRepository.findByNameContainingIgnoreCaseAndAtivoTrue(name);
    }

    public void validarArena(PartnerRegistrationDTO dto) {
        if (dto.getNomeArena() == null || dto.getNomeArena().isBlank())
            throw new IllegalArgumentException("Nome da arena é obrigatório");

        if (dto.getCnpjArena() == null)
            throw new IllegalArgumentException("CNPJ da arena é obrigatório");

        String cnpj = dto.getCnpjArena().replaceAll("\\D", "");
        if (cnpj.length() != 14)
            throw new IllegalArgumentException("CNPJ inválido");

        if (dto.getCepArena() == null) {
            throw new IllegalArgumentException("CEP é obrigatorio");
        }

        String cnpjLimpo = dto.getCnpjArena().replaceAll("\\D", "");

        if (cnpjLimpo.length() != 14) {
            throw new IllegalArgumentException("CNPJ inválido: deve conter 14 dígitos");
        }

        if (arenaRepository.existsByCnpj(cnpjLimpo)) {
            throw new IllegalArgumentException("CNPJ existente.");
        }
    }
}