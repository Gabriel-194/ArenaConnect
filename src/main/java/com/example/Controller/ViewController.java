package com.example.Controller;

import org.springframework.ui.Model;
import com.example.DTOs.PartnerRegistrationDTO;
import com.example.DTOs.UserRegistrationDTO;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    @GetMapping("/login")
    public String login(Model model) {
        return "login";
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/login";
    }

    @GetMapping("/register")
    public String showRegister(Model model) {
        model.addAttribute("userDTO", new UserRegistrationDTO());
        model.addAttribute("parceiroDTO", new PartnerRegistrationDTO());
        return "register";
    }

}