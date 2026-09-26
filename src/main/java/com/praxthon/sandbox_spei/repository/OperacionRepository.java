package com.praxthon.sandbox_spei.repository;

import com.praxthon.sandbox_spei.entity.Operacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OperacionRepository extends JpaRepository<Operacion, Long> {
    boolean existsByReferenciaSeguimiento(String referenciaSeguimiento);
}