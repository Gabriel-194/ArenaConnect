package com.example.Service;

import com.example.DTOs.ArenaConfigDTO;
import com.example.DTOs.ArenaDistanceDTO;
import com.example.DTOs.ArenaResponseDTO;
import com.example.DTOs.PartnerRegistrationDTO;
import com.example.Models.Arena;
import com.example.Models.Users;
import com.example.Repository.ArenaRepository;
import com.example.Repository.UserRepository;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ArenaService {

    @Autowired
    private ArenaRepository arenaRepository;

    private static final Logger logger = LoggerFactory.getLogger(ArenaService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

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

    public List<Arena> getArenaAtivo() {
        logger.info("Listando todas as arenas");
        return arenaRepository.findByAtivoTrue();
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

    public ArenaConfigDTO getArenaConfig(Integer idArena) {
        Arena arena = arenaRepository.findById(Long.valueOf(idArena))
                .orElseThrow(() -> new RuntimeException("Arena não encontrada"));

        ArenaConfigDTO dto = new ArenaConfigDTO();

        dto.setAbertura(arena.getHoraInicio());
        dto.setFechamento(arena.getHoraFim());

        if (arena.getDiasFuncionamento() != null && !arena.getDiasFuncionamento().isEmpty()) {
            dto.setDiasOperacao(Arrays.asList(arena.getDiasFuncionamento().split(",")));
        } else {
            dto.setDiasOperacao(new ArrayList<>());
        }

        return dto;
    }

    public void atualizarConfigArena(Integer idArena, ArenaConfigDTO dto) {
        Arena arena = arenaRepository.findById(Long.valueOf(idArena))
                .orElseThrow(() -> new RuntimeException("Arena não encontrada"));

        if(dto.getAbertura() != null) arena.setHoraInicio(dto.getAbertura());
        if(dto.getFechamento() != null) arena.setHoraFim(dto.getFechamento());

        if (dto.getDiasOperacao() != null && !dto.getDiasOperacao().isEmpty()) {
            String diasString = String.join(",", dto.getDiasOperacao());
            arena.setDiasFuncionamento(diasString);
        } else {
            arena.setDiasFuncionamento("");
        }
        arenaRepository.save(arena);
    }

    public List<ArenaDistanceDTO> buscarArenasInteligente(Double lat, Double lon, String search) {
        List<Object[]> rows;

        if (lat == null || lon == null) {
            rows = arenaRepository.findRecent(search);
        } else {
            rows = arenaRepository.findNearestWithDistance(lat, lon, search);
        }

        return rows.stream().map(row -> {
            ArenaDistanceDTO dto = new ArenaDistanceDTO();

            dto.setId(((Number) row[0]).longValue());
            dto.setName((String) row[1]);
            dto.setEndereco((String) row[2]);
            dto.setCidade((String) row[3]);
            dto.setEstado((String) row[4]);
            dto.setLatitude(row[5] != null ? ((Number) row[5]).doubleValue() : 0.0);
            dto.setLongitude(row[6] != null ? ((Number) row[6]).doubleValue() : 0.0);

            if(row[7] != null){
                double rawDistance = ((Number) row[7]).doubleValue();
                dto.setDistanceKm(applyTortuosity(rawDistance));
            } else {
                dto.setDistanceKm(null);
            }

            dto.setSchemaName((String) row[8]);

            if (row[9] != null) {
                dto.setHoraInicio(row[9].toString().substring(0, 5));
            }
            if (row[10] != null) {
                dto.setHoraFim(row[10].toString().substring(0, 5));
            }
            return dto;
        }).toList();
    }


    private double applyTortuosity(double km) {
        if (km < 2) return km * 1.1;
        if (km < 5) return km * 1.3;
        return km * 1;
    }

    public List<ArenaResponseDTO> findAllAdmin() {
        List<Arena> arenas = arenaRepository.findAll();

        return arenas.stream().map(arena -> {

            Users admin = userRepository.findFirstByIdArena(arena.getId()).orElse(null);

            String adminNome = (admin != null) ? admin.getNome() : "Sem Admin";
            String adminEmail = (admin != null) ? admin.getEmail() : "N/A";

            return new ArenaResponseDTO(
                    arena.getId(),
                    arena.getName(),
                    arena.getCnpj(),
                    arena.getCep(),
                    arena.getEndereco(),
                    arena.getCidade(),
                    arena.getEstado(),
                    arena.isAtivo(),
                    adminNome,
                    adminEmail
            );
        }).collect(Collectors.toList());
    }
}