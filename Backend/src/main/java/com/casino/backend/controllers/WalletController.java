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
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @GetMapping("/balance")
    public String getBalance() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).get();
        Wallet wallet = walletRepository.findByUserId(user.getId());
        if (wallet == null) return "El saldo actual de " + username + " es: $0.0";
        return "El saldo actual de " + username + " es: $" + wallet.getBalance();
    }

    @GetMapping("/history")
    public List<Transaction> getHistory() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).get();
        return transactionRepository.findByUserId(user.getId());
    }

    // DEPÓSITOS VIP BLINDADOS
    @PostMapping("/vip-deposit/{username}")
    public ResponseEntity<?> vipDeposit(@PathVariable String username, @RequestBody Map<String, Object> request) {
        try {
            Object cardNumberObj = request.get("cardNumber");
            if (cardNumberObj == null) return ResponseEntity.badRequest().body("Error: Faltan los 16 dígitos de tu tarjeta.");
            
            String cardNumber = cardNumberObj.toString().replaceAll("\\s+", ""); 
            if (!cardNumber.matches("\\d{16}")) return ResponseEntity.badRequest().body("Error: La tarjeta debe ser de exactamente 16 números.");

            Object amountObj = request.get("amount");
            if (amountObj == null) return ResponseEntity.badRequest().body("Error: No se envió la cantidad.");
            
            Double amount = Double.parseDouble(amountObj.toString());
            
            if (amount <= 0) {
                return ResponseEntity.badRequest().body("Error: Ingresa una cantidad mayor a $0.");
            }
            
            if (amount > 100000) {
                return ResponseEntity.badRequest().body("Error: El depósito máximo permitido por transacción es de $100,000.");
            }

            User user = userRepository.findByUsername(username).orElse(null);
            if (user == null) return ResponseEntity.badRequest().body("Error: El usuario " + username + " no existe.");
            
            Wallet wallet = walletRepository.findByUserId(user.getId());
            if (wallet == null) {
                wallet = new Wallet();
                wallet.setUser(user);
                wallet.setBalance(0.0);
            }

            wallet.setBalance(wallet.getBalance() + amount);
            walletRepository.save(wallet);
            transactionRepository.save(new Transaction("DEPOSITO", amount, wallet.getBalance(), user));

            String ultimos4 = cardNumber.substring(12);
            return ResponseEntity.ok("¡Éxito " + username + "! Has depositado $" + amount + " con la tarjeta terminando en " + ultimos4 + ".");

        } catch (Exception e) {
            return ResponseEntity.status(500).body("ERROR: " + e.getMessage());
        }
    }

    // RETIRO DE FONDOS (CASHOUT) CON TARJETA
    @PostMapping("/withdraw/{username}")
    public ResponseEntity<?> withdraw(@PathVariable String username, @RequestBody Map<String, Object> request) {
        try {
            Object cardNumberObj = request.get("cardNumber");
            if (cardNumberObj == null) return ResponseEntity.badRequest().body("Error: Faltan los 16 dígitos de tu tarjeta destino.");
            
            String cardNumber = cardNumberObj.toString().replaceAll("\\s+", ""); 
            if (!cardNumber.matches("\\d{16}")) return ResponseEntity.badRequest().body("Error: La tarjeta destino debe tener exactamente 16 números.");

            Object amountObj = request.get("amount");
            if (amountObj == null) return ResponseEntity.badRequest().body("Error: No se envió la cantidad a retirar.");
            
            Double amount = Double.parseDouble(amountObj.toString());
            if (amount <= 0) return ResponseEntity.badRequest().body("Error: Ingresa una cantidad mayor a $0 para retirar.");

            User user = userRepository.findByUsername(username).orElse(null);
            if (user == null) return ResponseEntity.badRequest().body("Error: El usuario no existe.");
            
            Wallet wallet = walletRepository.findByUserId(user.getId());
            if (wallet == null || wallet.getBalance() < amount) {
                return ResponseEntity.badRequest().body("Error: Fondos insuficientes. Tu saldo actual es menor a la cantidad solicitada.");
            }

            wallet.setBalance(wallet.getBalance() - amount);
            walletRepository.save(wallet);
            transactionRepository.save(new Transaction("RETIRO", -amount, wallet.getBalance(), user));

            String ultimos4 = cardNumber.substring(12);
            return ResponseEntity.ok("¡Retiro exitoso! Has enviado $" + amount + " a la tarjeta terminando en " + ultimos4 + ".");

        } catch (Exception e) {
            return ResponseEntity.status(500).body("ERROR interno: " + e.getMessage());
        }
    }

    // ADMINISTRADOR CON CONTRASEÑA SECRETA
    @PostMapping("/make-me-admin")
    public ResponseEntity<?> makeMeAdmin(@RequestBody Map<String, String> request) {
        String secretKey = request.get("secretKey");
        
        if ("casinobj101217".equals(secretKey)) {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Optional<User> userOpt = userRepository.findByUsername(username);
            
            if (!userOpt.isPresent()) {
                return ResponseEntity.badRequest().body("Error: No se encontró al usuario.");
            }
            
            User user = userOpt.get();
            user.setRole("ADMIN"); 
            userRepository.save(user); 
            
            return ResponseEntity.ok("¡Felicidades " + username + "! Ahora eres ADMIN. Cierra sesión y vuelve a entrar.");
        }
        
        return ResponseEntity.status(403).body("Error: Contraseña secreta incorrecta.");
    }

    // RESULTADO DEL JUEGO (SUMAR O RESTAR APUESTA)
    @PostMapping("/game-result")
    public ResponseEntity<?> gameResult(@RequestBody Map<String, Double> request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).get();
        Wallet wallet = walletRepository.findByUserId(user.getId());
        
        if (wallet == null) {
            wallet = new Wallet();
            wallet.setUser(user);
            wallet.setBalance(0.0);
        }
        
        Double amount = request.get("amount"); 
        wallet.setBalance(wallet.getBalance() + amount);
        walletRepository.save(wallet);
        
        String tipo = amount > 0 ? "GANANCIA BLACKJACK" : "PERDIDA BLACKJACK";
        transactionRepository.save(new Transaction(tipo, amount, wallet.getBalance(), user));
        
        return ResponseEntity.ok(wallet.getBalance());
    }
}