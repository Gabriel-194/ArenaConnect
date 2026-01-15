package com.example.Repository;

import com.example.Models.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<Users, Integer> {

    Optional<Users> findByEmail(String email);

    Optional<Users> findByCpf(String cpf);

    boolean existsByEmail(String email);

    boolean existsByCpf(String cpf);

    List<Users> findByIdArena(Integer idArena);

    List<Users> findByRole(String role);

    List<Users> findByRoleAndAtivoTrue(String role);

    Optional<Users> findByEmailAndAtivoTrue(String email);
}