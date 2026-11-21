package com.kaiju.backend.repository;

import com.kaiju.backend.model.Movimiento;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimientoRepository extends JpaRepository<Movimiento, Long> {

    List<Movimiento> findByProductoIdOrderByFechaDescIdDesc(Long productoId);

    List<Movimiento> findAllByOrderByFechaDescIdDesc();

    void deleteByProductoId(Long productoId);
}
