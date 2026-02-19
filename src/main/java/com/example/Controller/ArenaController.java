package com.example.Controller;

import com.example.DTOs.ArenaConfigDTO;
import com.example.DTOs.ArenaDistanceDTO;
import com.example.Models.Arena;
import com.example.Service.ArenaService;
import com.example.Service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController()
@RequestMapping("/api/arena")
public class ArenaController {

    @Autowired
    private ArenaService arenaService;

    @Autowired
    private JwtService jwtService;

    @GetMapping
    public ResponseEntity<List<ArenaDistanceDTO>> listarArenas(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon,
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(
                arenaService.buscarArenasInteligente(lat, lon, search)
        );
    }


    @GetMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ArenaConfigDTO> getArenaConfig(@CookieValue("accessToken") String token) {
        Integer arenaId = jwtService.getArenaIdFromToken(token);

        if (arenaId == null) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(arenaService.getArenaConfig(arenaId));

    }

    @PutMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateConfig(@CookieValue("accessToken") String token,
                                          @RequestBody ArenaConfigDTO dto) {
        Integer arenaId = jwtService.getArenaIdFromToken(token);
        if (arenaId == null) {
            return ResponseEntity.badRequest().body("Token inválido ou sem arena vinculada.");
        }

        try {
            arenaService.atualizarConfigArena(arenaId, dto);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro: " + e.getMessage());
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> listarArenas() {
        return ResponseEntity.ok(arenaService.findAllAdmin());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> changeStatus(@PathVariable Long id,@RequestParam boolean ativo){
        arenaService.changeStatusArena(id,ativo);

        String mensagem = ativo ? "arena ativada com sucesso" : "arena desativada com sucesso";
        return ResponseEntity.ok("{\"message\": \"" + mensagem + "\"}");
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateArena(@PathVariable Long id,@RequestBody Arena updateArena) {
        try{
            arenaService.updateArena(id, updateArena);
        return ResponseEntity.ok().body("{\"message\": \"Usuário atualizado com sucesso!\"}");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("{\"message\": \"" + e.getMessage() + "\"}");
        }
    }
}

