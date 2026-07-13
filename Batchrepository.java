package com.shelfwise.repository;

import com.shelfwise.entity.Batch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BatchRepository extends JpaRepository<Batch, Long> {

    @Query("select b from Batch b where b.currentQty > 0 and b.expiryDate <= :cutoff order by b.expiryDate asc")
    List<Batch> findNearExpiry(@Param("cutoff") LocalDate cutoff);

    @Query("select b from Batch b where b.currentQty > 0 and b.expiryDate < :today")
    List<Batch> findExpiredWithRemainingStock(@Param("today") LocalDate today);

    @Query("select coalesce(sum(b.initialQty), 0) from Batch b")
    long sumInitialQty();

    @Query("select coalesce(sum(b.currentQty), 0) from Batch b")
    long sumCurrentQty();
}
