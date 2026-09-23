package com.casino.backend.repositories;

import com.casino.backend.models.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    // Permite buscar todas las transacciones hechas por un usuario específico
    List<Transaction> findByUserId(Long userId);
}