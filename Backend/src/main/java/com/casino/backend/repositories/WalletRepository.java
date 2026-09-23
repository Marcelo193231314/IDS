package com.casino.backend.repositories;

import com.casino.backend.models.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    // Nos permitirá buscar cuánto dinero tiene un usuario usando su ID
    Wallet findByUserId(Long userId);
}