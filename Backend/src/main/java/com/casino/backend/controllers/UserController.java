package com.casino.backend.controllers;

import com.casino.backend.models.Transaction;
import com.casino.backend.models.User;
import com.casino.backend.models.Wallet;
import com.casino.backend.repositories.TransactionRepository;
import com.casino.backend.repositories.UserRepository;
import com.casino.backend.repositories.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private WalletRepository walletRepository;

    @PutMapping("/update-username")
    public ResponseEntity<String> updateUsername(@RequestParam String newUsername) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        
        if (userRepository.findByUsername(newUsername).isPresent()) {
            return ResponseEntity.badRequest().body("Error: El nombre de usuario '" + newUsername + "' ya está ocupado.");
        }
        
        User user = userRepository.findByUsername(currentUsername).get();
        user.setUsername(newUsername);
        userRepository.save(user);
        
        return ResponseEntity.ok("Nombre actualizado con éxito a: " + newUsername + ". (Nota: Deberás hacer Login de nuevo con tu nuevo nombre).");
    }

    @DeleteMapping("/delete-account")
    public ResponseEntity<String> deleteMyAccount() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Optional<User> userOpt = userRepository.findByUsername(username);
            
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                
                List<Transaction> transactions = transactionRepository.findByUserId(user.getId());
                if (transactions != null && !transactions.isEmpty()) {
                    transactionRepository.deleteAll(transactions);
                }

                Wallet wallet = walletRepository.findByUserId(user.getId());
                if (wallet != null) {
                    walletRepository.delete(wallet);
                }

                userRepository.delete(user);
                
                return ResponseEntity.ok("Tu cuenta ha sido eliminada permanentemente. ¡Hasta pronto!");
            }
            
            return ResponseEntity.badRequest().body("Error: Usuario no encontrado.");
            
        } catch (Exception e) {
            return ResponseEntity.status(400).body("Error: No se pudo borrar tu cuenta debido a un conflicto en la base de datos.");
        }
    }
}