package com.example.Repository;

import com.example.Models.Users;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<Users, Integer> {

    Optional<Users> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByCpf(String cpf);

    Optional<Users> findFirstByIdArena(Integer idArena);

    @Modifying
    @Transactional
    @Query("UPDATE Users u SET u.senhaHash = :newPassword WHERE u.email = :email")
    void updatePassword(@Param("email") String email, @Param("newPassword") String newPassword);
}