package com.shelfwise.repository;

import com.shelfwise.model.Alert;
import com.shelfwise.model.Alert.AlertType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findByIsResolvedFalseOrderByCreatedAtDesc();

    List<Alert> findByTypeAndIsResolvedFalse(AlertType type);

    List<Alert> findByProductIdAndIsResolvedFalse(Long productId);

    @Modifying
    @Transactional
    @Query("UPDATE Alert a SET a.isResolved = true WHERE a.productId = :productId AND a.type = :type")
    int resolveAlertsForProduct(Long productId, AlertType type);

    @Modifying
    @Transactional
    @Query("DELETE FROM Alert a WHERE a.isResolved = true")
    void deleteResolvedAlerts();
}
