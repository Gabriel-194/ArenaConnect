package com.example.Service;

import com.example.DTOs.PartnerRegistrationDTO;
import com.example.DTOs.UserRegistrationDTO;
import com.example.Domain.RoleEnum;
import com.example.Models.Users;
import com.example.Models.Arena;
import com.example.Repository.UserRepository;
import com.example.Repository.ArenaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ArenaRepository arenaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    @Autowired
    private ArenaService arenaService;

    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean cpfExists(String cpf) {
        return userRepository.existsByCpf(cpf);
    }

    @Transactional
    public Users registrarCliente(UserRegistrationDTO dto){
            validarDados(dto);

            String senhaHash = passwordEncoder.encode(dto.getSenha());

            Users user = new Users(
                    dto.getNome(),
                    RoleEnum.CLIENTE,
                    dto.getEmail(),
                    senhaHash,
                    dto.getCpfLimpo(),
                    true,
                    dto.getTelefoneLimpo(),
                    dto.getIdArena()
            );

            Users savedUser = userRepository.save(user);

            logger.info("✅ Cliente criado com sucesso: {}", savedUser.getEmail());

            return savedUser;
    }



    @Transactional
    public Users registerPartner(PartnerRegistrationDTO dto) {
        validarAdmin(dto);
        arenaService.validarArena(dto);

        //Arena create
        Arena arena = new Arena();
        arena.setName(dto.getNomeArena());
        arena.setCnpj(dto.getCnpjArena().replaceAll("\\D", ""));
        arena.setCep(dto.getCepArena().replaceAll("\\D", ""));
        arena.setEndereco(dto.getEnderecoArena());
        arena.setCidade(dto.getCidadeArena());
        arena.setEstado(dto.getEstadoArena());
        arena.setAtivo(true);
        String schemaName = (dto.getNomeArena());
        arena.setSchemaName(schemaName);
        arena.setLatitude(dto.getLatitude());
        arena.setLongitude(dto.getLongitude());

        arenaService.cadastrarArena(arena);

        //admin create
        Users admin = new Users();
        admin.setNome(dto.getNomeUser());
        admin.setEmail(dto.getEmailAdmin());
        admin.setCpf(dto.getCpfUser().replaceAll("\\D", ""));
        admin.setTelefone(dto.getTelefoneUser() != null ?
                dto.getTelefoneUser().replaceAll("\\D", "") : null);

        admin.setSenhaHash(passwordEncoder.encode(dto.getSenhaAdmin()));
        admin.setRole(RoleEnum.ADMIN);
        admin.setIdArena(arena.getId());
        admin.setAtivo(true);

        return userRepository.save(admin);
    }

    //validations
    private void validarDados(UserRegistrationDTO dto) {

        if (dto == null) {
            throw new IllegalArgumentException("Dados não informados");
        }

        if (dto.getNome() == null || dto.getNome().isBlank()) {
            throw new IllegalArgumentException("Nome é obrigatório");
        }

        if (dto.getEmail() == null || dto.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email é obrigatório");
        }

        if (!dto.getSenha().equals(dto.getConfirmarSenha())) {
            throw new IllegalArgumentException("Senhas não conferem");
        }

        if (dto.getEmail() == null || dto.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email é obrigatório");
        }

        if (!UserRegistrationDTO.isEmailValid(dto.getEmail())) {
            throw new IllegalArgumentException("Email inválido");
        }

        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email já cadastrado");
        }

        String cpfLimpo = dto.getCpfLimpo();
        if (cpfLimpo == null || cpfLimpo.length() != 11) {
            throw new IllegalArgumentException("CPF inválido");
        }

        if (cpfLimpo == null || cpfLimpo.isBlank()) {
            throw new IllegalArgumentException("CPF é obrigatório");
        }

        if (userRepository.existsByCpf(cpfLimpo)) {
            throw new IllegalArgumentException("CPF já cadastrado");
        }
    }

    public void validarAdmin(PartnerRegistrationDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Dados não informados");
        }

        if (dto.getNomeUser() == null || dto.getNomeUser().isBlank()) {
            throw new IllegalArgumentException("Nome é obrigatório");
        }

        if (dto.getEmailAdmin() == null || dto.getEmailAdmin().isBlank()) {
            throw new IllegalArgumentException("Email é obrigatório");
        }

        if (!UserRegistrationDTO.isEmailValid(dto.getEmailAdmin())) {
            throw new IllegalArgumentException("Email inválido");
        }

        if (userRepository.existsByEmail(dto.getEmailAdmin())) {
            throw new IllegalArgumentException("Email já cadastrado");
        }

        if (!dto.getSenhaAdmin().equals(dto.getConfirmarSenha())) {
            throw new IllegalArgumentException("Senhas não conferem");
        }

        String cpfLimpo = dto.getCpfLimpo();
        if (cpfLimpo == null || cpfLimpo.length() != 11) {
            throw new IllegalArgumentException("CPF inválido");
        }

        if (cpfLimpo == null || cpfLimpo.isBlank()) {
            throw new IllegalArgumentException("CPF é obrigatório");
        }

        if (userRepository.existsByCpf(cpfLimpo)) {
            throw new IllegalArgumentException("CPF já cadastrado");
        }
    }

}