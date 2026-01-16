package com.example.Controller;

import com.example.DTOs.PartnerRegistrationDTO;
import com.example.DTOs.UserRegistrationDTO;
import com.example.Service.UserService;
import com.example.Service.ArenaService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private ArenaService arenaService;


    @PostMapping("/register")
    public String registerUser(
            @Valid @ModelAttribute("userDTO") UserRegistrationDTO dto,
            BindingResult bindingResult,
            Model model,
            @RequestParam(name = "confirmPassword", required = false) String confirmPassword,
            RedirectAttributes redirectAttributes
    ) {

        model.addAttribute("parceiroDTO", new PartnerRegistrationDTO());

        if (bindingResult.hasErrors()) {
            model.addAttribute("tipoCadastro", "cliente");
            return "register";
        }

        boolean success = userService.registerCliente(
                dto,
                confirmPassword,
                bindingResult
        );

        if (!success || bindingResult.hasErrors()) {
            model.addAttribute("tipoCadastro", "cliente");
            return "register";
        }

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Cadastro realizado com sucesso! Você já pode fazer login."
        );

        return "redirect:/login";
    }


    @PostMapping("/register-partner")
    public String registerParceiro(
            @Valid @ModelAttribute("parceiroDTO") PartnerRegistrationDTO dto,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes,
            @RequestParam(name = "confirmaSenhaAdmin", required = false) String confirmaSenha) {

        logger.info("Tentativa de registro de PARCEIRO: {}", dto.getEmailAdmin());


        model.addAttribute("userDTO", new UserRegistrationDTO());


        if (bindingResult.hasErrors()) {
            logger.warn("Erros de validação encontrados: {}", bindingResult.getAllErrors());
            model.addAttribute("erro", "Preencha todos os campos obrigatórios corretamente.");
            model.addAttribute("tipoCadastro", "parceiro");
            return "register";
        }

        try {

            if (userService.emailExists(dto.getEmailAdmin())) {
                model.addAttribute("erro", "Este email já está cadastrado.");
                logger.warn("Tentativa de registro com email duplicado: {}", dto.getEmailAdmin());
                model.addAttribute("tipoCadastro", "parceiro");
                return "register";
            }


            String cpfLimpo = dto.getCpfUser().replaceAll("\\D", "");
            if (userService.cpfExists(cpfLimpo)) {
                model.addAttribute("erro", "Este CPF já está cadastrado.");
                model.addAttribute("tipoCadastro", "parceiro");

                logger.warn("Tentativa de registro com CPF duplicado: {}", cpfLimpo);
                return "register";
            }

            String cnpjLimpo = dto.getCnpjArena().replaceAll("\\D", "");
            if (arenaService.cnpjExists(cnpjLimpo)) {
                model.addAttribute("erro", "Este CNPJ já está cadastrado.");
                model.addAttribute("tipoCadastro", "parceiro");
                logger.warn("Tentativa de registro com CNPJ duplicado: {}", cnpjLimpo);
                return "register";
            }


            if (confirmaSenha != null && !dto.getSenhaAdmin().equals(confirmaSenha)) {
                model.addAttribute("erro", "As senhas não coincidem.");
                model.addAttribute("tipoCadastro", "parceiro");
                logger.warn("Senhas não coincidem para parceiro: {}", dto.getEmailAdmin());
                return "register";
            }

            userService.registerParceiro(dto);
            logger.info("✅ Parceiro registrado com sucesso: {} - Arena: {}",
                    dto.getEmailAdmin(), dto.getNomeArena());

            redirectAttributes.addFlashAttribute("successMessage",
                    "Cadastro de parceiro realizado com sucesso! Aguarde a aprovação.");

            return "redirect:/login";

        } catch (Exception e) {
            logger.error("❌ Erro ao registrar parceiro: {}", e.getMessage(), e);
            model.addAttribute("erro",
                    "Ocorreu um erro ao processar seu cadastro. Por favor, tente novamente.");
            model.addAttribute("tipoCadastro", "parceiro");

            return "register";
        }
    }
}