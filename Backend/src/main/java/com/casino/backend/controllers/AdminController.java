package com.casino.backend.controllers;

import com.casino.backend.models.Transaction;
import com.casino.backend.models.User;
import com.casino.backend.models.Wallet;
import com.casino.backend.repositories.TransactionRepository;
import com.casino.backend.repositories.UserRepository;
import com.casino.backend.repositories.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @GetMapping("/transactions/all")
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    // Ruta actualizada con :.+ para soportar correos (ej. juan@gmail.com)
    @GetMapping("/player/{username:.+}")
    public ResponseEntity<?> getPlayerDetails(@PathVariable String username) {
        try {
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (!userOpt.isPresent()) {
                return ResponseEntity.badRequest().body("Error: Usuario no encontrado.");
            }
            
            User user = userOpt.get();
            
            Wallet wallet = null;
            try {
                wallet = walletRepository.findByUserId(user.getId());
            } catch (Exception e) {
                System.out.println("Aviso: El usuario " + username + " tiene un conflicto en la billetera.");
            }

            List<Transaction> transactions = transactionRepository.findByUserId(user.getId());

            // Empaquetamos las transacciones en un formato seguro para evitar el Error 500
            List<Map<String, Object>> safeTransactions = new ArrayList<>();
            if (transactions != null) {
                for (Transaction t : transactions) {
                    Map<String, Object> tMap = new HashMap<>();
                    tMap.put("type", t.getType());
                    tMap.put("amount", t.getAmount());
                    tMap.put("balanceAfter", t.getBalanceAfter());
                    // Convertimos la fecha a texto seguro
                    tMap.put("timestamp", t.getTimestamp() != null ? t.getTimestamp().toString() : "");
                    safeTransactions.add(tMap);
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("username", user.getUsername());
            response.put("role", user.getRole());
            response.put("balance", wallet != null ? wallet.getBalance() : 0.0);
            response.put("blocked", user.isBlocked());
            response.put("transactions", safeTransactions);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace(); 
            return ResponseEntity.status(500).body("Error interno: " + e.getMessage());
        }
    }

    @GetMapping("/players")
    public Object getAllPlayers() {
        return userRepository.findAll();
    }

    @DeleteMapping("/delete-user/{username}")
    public ResponseEntity<String> deletePlayerAsAdmin(@PathVariable String username) {
        try {
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (!userOpt.isPresent()) {
                return ResponseEntity.badRequest().body("Error: Usuario no encontrado.");
            }
            
            User user = userOpt.get();
            
            if ("ADMIN".equals(user.getRole()) || "ROLE_ADMIN".equals(user.getRole())) {
                return ResponseEntity.badRequest().body("Error: No puedes eliminar a otro Administrador.");
            }
            
            List<Transaction> transactions = transactionRepository.findByUserId(user.getId());
            if (transactions != null && !transactions.isEmpty()) {
                transactionRepository.deleteAll(transactions);
            }

            Wallet wallet = walletRepository.findByUserId(user.getId());
            if (wallet != null) {
                walletRepository.delete(wallet);
            }
            
            userRepository.delete(user);
            
            return ResponseEntity.ok("El jugador " + username + " ha sido eliminado permanentemente.");
            
        } catch (Exception e) {
            return ResponseEntity.status(400).body("Error: No se pudo borrar el usuario debido a un conflicto con registros antiguos en la base de datos.");
        }
    }
}