package com.jomariabejo.connectly_api.supplier_api.repository;

import com.jomariabejo.connectly_api.supplier_api.entity.GRNItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GRNItemRepository extends JpaRepository<GRNItem, Long> {
    @Query("SELECT gi FROM GRNItem gi WHERE gi.grn.id = :grnId ORDER BY gi.createdAt ASC")
    List<GRNItem> findByGrn(@Param("grnId") Long grnId);

    @Query("SELECT gi FROM GRNItem gi WHERE gi.poItem.id = :poItemId ORDER BY gi.createdAt DESC")
    List<GRNItem> findByPoItem(@Param("poItemId") Long poItemId);
}
