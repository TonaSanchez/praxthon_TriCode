package com.praxthon.sandbox_spei.repository;

import com.praxthon.sandbox_spei.entity.Operacion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OperacionRepository extends JpaRepository<Operacion, Long> {
    boolean existsByReferenciaSeguimiento(String referenciaSeguimiento);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Operacion o where o.id = :id")
    Optional<Operacion> buscarParaActualizar(@Param("id") Long id);
}