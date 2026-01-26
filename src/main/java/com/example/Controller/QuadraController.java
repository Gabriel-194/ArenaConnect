package com.example.Controller;

import com.example.Models.Quadra;
import com.example.Service.QuadraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quadra")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN','CLIENTE')")
public class QuadraController {

    @Autowired
    private QuadraService quadraService;

    @PostMapping("/createQuadra")
    public ResponseEntity<Quadra> createQuadra(@RequestBody Quadra quadra) {
        try {
            Quadra novaQuadra = quadraService.cadastrar(quadra);
            return ResponseEntity.ok(novaQuadra);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<Quadra>> getAllQuadra() {
        List<Quadra> quadras = quadraService.getAll();
        return ResponseEntity.ok(quadras);
    }

    @GetMapping("/courtAtivas")
    public ResponseEntity<List<Quadra>> getCourtAtivas() {
        List<Quadra> quadras = quadraService.getAtivasPorArena();
        return ResponseEntity.ok(quadras);
    }

    @PostMapping("/changeStatusQuadra")
    public ResponseEntity<Quadra> changeStatusQuadra(@RequestBody Quadra quadra) {
        quadraService.alterarStatus(quadra.getId());
        return ResponseEntity.ok().build();
    }

    @PutMapping
    public  ResponseEntity<Quadra> updateQuadra(@RequestBody Quadra quadra) {
        try{
            Quadra atualizada= quadraService.atualizar(quadra);
            return ResponseEntity.ok(atualizada);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Quadra> getQuadraById(@PathVariable Integer id) {
        return quadraService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


}
