package com.example.Service;

import com.example.DTOs.PartnerRegistrationDTO;
import com.example.DTOs.UserRegistrationDTO;
import com.example.Models.Users;
import com.example.Models.Arena;
import com.example.Repository.UserRepository;
import com.example.Repository.ArenaRepository;
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


    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean cpfExists(String cpf) {
        return userRepository.existsByCpf(cpf);
    }

    @Transactional
    public boolean registerCliente(UserRegistrationDTO dto, String confirmPassword, BindingResult bindingResult) {
        if (confirmPassword != null && !dto.getSenha().equals(confirmPassword)) {
            bindingResult.rejectValue(
                    "senha",
                    "error.senha",
                    "As senhas não coincidem"
            );
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            bindingResult.rejectValue(
                    "email",
                    "error.email",
                    "Este e-mail já está cadastrado"
            );
        }
        String cpf = dto.getCpfLimpo();
        if (cpf != null && userRepository.existsByCpf(cpf)) {
            bindingResult.rejectValue(
                    "cpf",
                    "error.cpf",
                    "Este CPF já está cadastrado"
            );
        }

        if (bindingResult.hasErrors()) {
            return false;
        }

        Users user = new Users();
        user.setNome(dto.getNome());
        user.setEmail(dto.getEmail());
        user.setTelefone(dto.getTelefoneLimpo());
        user.setCpf(cpf);
        user.setSenhaHash(passwordEncoder.encode(dto.getSenha()));
        user.setRole("CLIENTE");
        user.setAtivo(true);
        user.setIdArena(null);

        userRepository.save(user);
        return true;
    }



    @Transactional
    public Users registerParceiro(PartnerRegistrationDTO dto) {
        Arena arena = new Arena();
        arena.setName(dto.getNomeArena());
        arena.setCnpj(dto.getCnpjArena().replaceAll("\\D", ""));
        arena.setCep(dto.getCepArena().replaceAll("\\D", ""));
        arena.setEndereco(dto.getEnderecoArena());
        arena.setCidade(dto.getCidadeArena());
        arena.setEstado(dto.getEstadoArena());
        arena.setAtivo(true);
        String schemaName = generateSchemaName(dto.getNomeArena());
        arena.setSchemaName(schemaName);
        Arena arenaSalva = arenaRepository.save(arena);

        Users admin = new Users();
        admin.setNome(dto.getNomeUser());
        admin.setEmail(dto.getEmailAdmin());
        admin.setCpf(dto.getCpfUser().replaceAll("\\D", ""));
        admin.setTelefone(dto.getTelefoneUser() != null ?
                dto.getTelefoneUser().replaceAll("\\D", "") : null);

        admin.setSenhaHash(passwordEncoder.encode(dto.getSenhaAdmin()));
        admin.setRole("ADMIN");
        admin.setIdArena(arenaSalva.getId());
        admin.setAtivo(true);

        return userRepository.save(admin);
    }


    private String generateSchemaName(String nomeArena) {
        // Remove acentos e caracteres especiais
        String schemaName = nomeArena
                .toLowerCase()
                .replaceAll("[áàãâä]", "a")
                .replaceAll("[éèêë]", "e")
                .replaceAll("[íìîï]", "i")
                .replaceAll("[óòõôö]", "o")
                .replaceAll("[úùûü]", "u")
                .replaceAll("[ç]", "c")
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", "_")
                .trim();

        long timestamp = System.currentTimeMillis();

        if (schemaName.length() > 40) {
            schemaName = schemaName.substring(0, 40);
        }

        return schemaName + "_" + timestamp;
    }

    public Optional<Users> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<Users> findById(Integer id) {
        return userRepository.findById(id);
    }

    public Optional<Users> findByCpf(String cpf) {
        return userRepository.findByCpf(cpf);
    }

    @Transactional
    public Users updateUser(Integer id, UserRegistrationDTO dto) {
        Users user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        user.setNome(dto.getNome());
        user.setTelefone(dto.getTelefoneLimpo());

        if (dto.getCpf() != null && !dto.getCpf().isEmpty()) {
            user.setCpf(dto.getCpfLimpo());
        }

        if (dto.getSenha() != null && !dto.getSenha().isEmpty()) {
            user.setSenhaHash(passwordEncoder.encode(dto.getSenha()));
        }

        return userRepository.save(user);
    }

    @Transactional
    public void desactivateUser(Integer id) {
        Users user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        user.setAtivo(false);
        userRepository.save(user);
    }

    @Transactional
    public void activateUser(Integer id) {
        Users user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        user.setAtivo(true);
        userRepository.save(user);
    }


    public Optional<Users> findByEmailAndActive(String email) {
        return userRepository.findByEmailAndAtivoTrue(email);
    }

}