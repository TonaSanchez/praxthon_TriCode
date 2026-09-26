package com.praxthon.sandbox_spei.repository;

import com.praxthon.sandbox_spei.entity.ClaveIdempotencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClaveIdempotenciaRepository extends JpaRepository<ClaveIdempotencia, String> {
}