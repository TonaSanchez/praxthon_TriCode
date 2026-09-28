package com.praxthon.sandbox_spei.repository;

import com.praxthon.sandbox_spei.entity.Transicion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TransicionRepository extends JpaRepository<Transicion, Long> {
    List<Transicion> findByOperacionIdOrderByFechaAscIdAsc(Long operacionId);
}