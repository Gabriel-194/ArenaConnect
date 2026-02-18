package com.example.Service;

import com.example.DTOs.PartnerRegistrationDTO;
import com.example.DTOs.UserRegistrationDTO;
import com.example.DTOs.UserResponseDTO;
import com.example.Domain.RoleEnum;
import com.example.Exceptions.AsaasIntegrationException;
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

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private AsaasService asaasService;

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
            validarDados(
                    dto.getEmail(),
                    dto.getSenha(),
                    dto.getConfirmarSenha(),
                    dto.getCpfLimpo(),
                    dto.getNome()
            );

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
        validarDados(
                dto.getEmailAdmin(),
                dto.getSenhaAdmin(),
                dto.getConfirmarSenha(),
                dto.getCpfLimpo(),
                dto.getNomeUser()
        );
        arenaService.validarArena(dto);

        //Arena create
        Arena arena = new Arena();
        arena.setName(dto.getNomeArena());
        arena.setCnpj(dto.getCnpjArena().replaceAll("\\D", ""));
        arena.setCep(dto.getCepArena().replaceAll("\\D", ""));
        arena.setEndereco(dto.getEnderecoArena());
        arena.setCidade(dto.getCidadeArena());
        arena.setEstado(dto.getEstadoArena());
        arena.setAtivo(false);
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

        Users savedAdmin = userRepository.save(admin);

        String asaasCustomerId = null;

        try {
            asaasCustomerId = asaasService.createCustomer(savedAdmin);
            savedAdmin.setAsaasCustomerId(asaasCustomerId);

            String walletId = asaasService.createWallet(dto);
            arena.setAsaasWalletId(walletId);

            String subscriptionId = asaasService.createSubscription(asaasCustomerId);
            arena.setAssasSubscriptionId(subscriptionId);

            arenaRepository.save(arena);
            userRepository.save(savedAdmin);

            logger.info("✅ Integração Asaas Completa: Cust={} Wallet={}", asaasCustomerId, walletId);

        } catch (Exception e) {
            logger.error("❌ Erro no Asaas. Iniciando rollback manual...", e);

            if (asaasCustomerId != null) {
                asaasService.deleteCustomer(asaasCustomerId);
            }
            String errorMessage = e.getMessage();

            if (errorMessage != null && errorMessage.contains("email")) {
                throw new AsaasIntegrationException("Email já cadastrado no Asaas");
            }

            if (errorMessage != null && errorMessage.contains("número")) {
                throw new AsaasIntegrationException("Telefone inválido");
            }

            throw new AsaasIntegrationException("Erro ao integrar com sistema de pagamento");

        }

        return savedAdmin;
    }

    //validations
    private void validarDados(String email, String senha, String confirmarSenha, String cpfLimpo,String nome) {

        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome é obrigatório");
        }

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email é obrigatório");
        }

        if (!UserRegistrationDTO.isEmailValid(email)) {
            throw new IllegalArgumentException("Email inválido");
        }

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email já cadastrado");
        }

        if (!senha.equals(confirmarSenha)) {
            throw new IllegalArgumentException("Senhas não conferem");
        }

        if (cpfLimpo == null || cpfLimpo.isBlank()) {
            throw new IllegalArgumentException("CPF é obrigatório");
        }

        if (cpfLimpo.length() != 11) {
            throw new IllegalArgumentException("CPF inválido");
        }

        if (userRepository.existsByCpf(cpfLimpo)) {
            throw new IllegalArgumentException("CPF já cadastrado");
        }
    }

    public List<UserResponseDTO> findAll(){
        return userRepository.findAll().stream()
                .filter(user -> !RoleEnum.SUPERADMIN.equals(user.getRole()))
                .map(user -> new UserResponseDTO(
                        user.getIdUser(),
                        user.getNome(),
                        user.getEmail(),
                        user.getCpf(),
                        user.getTelefone(),
                        user.getRole()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteUser(Integer idUser) {
        Users user = userRepository.findById(idUser)
                .orElseThrow(() -> new AsaasIntegrationException("usuario nao encontrado"));

        user.setAtivo(false);
        userRepository.save(user);
    }

}