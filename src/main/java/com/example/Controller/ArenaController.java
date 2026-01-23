package com.example.Controller;

import com.example.Models.Arena;
import com.example.Models.Quadra;
import com.example.Service.ArenaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController()
@RequestMapping("/api/arena")
@PreAuthorize("hasAnyRole('CLIENTE', 'SUPERADMIN')")
public class ArenaController {

    @Autowired
    private ArenaService arenaService;

    @GetMapping
    public ResponseEntity<List<Arena>> getArenaAtivo() {
        List<Arena> arenas = arenaService.getArenaAtivo();
        return ResponseEntity.ok(arenas);

    }
}
