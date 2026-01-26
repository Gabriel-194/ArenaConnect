package com.example.Repository;

import com.example.Models.Quadra;
import com.example.Repository.Custom.QuadraRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuadraRepository extends JpaRepository<Quadra, Integer>, QuadraRepositoryCustom {

}
