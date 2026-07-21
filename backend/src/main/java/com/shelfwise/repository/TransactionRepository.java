package com.shelfwise.repository;

import com.shelfwise.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionId(String transactionId);

    List<Transaction> findByIsFraudTrue();

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.isFraud = true")
    Long countFraudulentTransactions();

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.isFraud = true")
    Double sumFraudulentAmount();

    @Query("SELECT t FROM Transaction t ORDER BY t.createdAt DESC")
    List<Transaction> findRecentTransactions(org.springframework.data.domain.Pageable pageable);
}
