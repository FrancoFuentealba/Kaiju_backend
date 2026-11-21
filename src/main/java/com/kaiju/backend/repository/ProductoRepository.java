package com.kaiju.backend.repository;

import com.kaiju.backend.model.Producto;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    boolean existsByCodigo(String codigo);

    // Para editar: ignora al propio producto.
    boolean existsByCodigoAndIdNot(String codigo, Long id);

    // Búsqueda por nombre, código o categoría (q = "" trae todo) y filtro opcional de stock bajo.
    @Query("""
            select p from Producto p
            where (lower(p.nombre) like lower(concat('%', :q, '%'))
                or lower(p.codigo) like lower(concat('%', :q, '%'))
                or lower(p.categoria) like lower(concat('%', :q, '%')))
              and (:soloBajo = false or p.stock <= p.stockMinimo)
            order by lower(p.nombre)
            """)
    List<Producto> buscar(@Param("q") String q, @Param("soloBajo") boolean soloBajo);

    // Bloquea la fila mientras se registra un movimiento (evita que dos ventas simultáneas rompan el stock).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Producto p where p.id = :id")
    Optional<Producto> buscarParaActualizar(@Param("id") Long id);
}
