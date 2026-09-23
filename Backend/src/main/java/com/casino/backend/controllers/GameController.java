package com.casino.backend.controllers;

import com.casino.backend.models.BetRequest;
import com.casino.backend.models.User;
import com.casino.backend.models.Wallet;
import com.casino.backend.repositories.UserRepository;
import com.casino.backend.repositories.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Random;

@RestController
@RequestMapping("/api/games")
public class GameController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @PostMapping("/coinflip")
    public String playCoinFlip(@RequestBody BetRequest betRequest) {
        
        // 1. Saber quién está jugando usando su gafete (Token JWT)
        // ¡Así evitamos que un usuario intente apostar con el dinero de otro!
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).get();
        Wallet wallet = walletRepository.findByUserId(user.getId());

        // 2. Validar que tenga suficiente dinero
        if (wallet.getBalance() < betRequest.getAmount()) {
            return "Error: Saldo insuficiente. Tienes $" + wallet.getBalance();
        }

        // 3. Cobrar la apuesta por adelantado
        wallet.setBalance(wallet.getBalance() - betRequest.getAmount());

        // 4. Tirar la moneda al azar (0 = CARA, 1 = CRUZ)
        String[] sides = {"CARA", "CRUZ"};
        String result = sides[new Random().nextInt(2)];

        // 5. Verificar si el jugador ganó
        if (result.equalsIgnoreCase(betRequest.getGuess())) {
            double ganancia = betRequest.getAmount() * 2; // Gana el doble
            wallet.setBalance(wallet.getBalance() + ganancia);
            walletRepository.save(wallet); // Guardamos el nuevo saldo en PostgreSQL
            
            return "¡Felicidades! Salió " + result + ". Ganaste $" + ganancia + ". Nuevo saldo: $" + wallet.getBalance();
        } else {
            walletRepository.save(wallet); // Guardamos la pérdida en PostgreSQL
            
            return "Qué lástima, salió " + result + ". Perdiste $" + betRequest.getAmount() + ". Nuevo saldo: $" + wallet.getBalance();
        }
    }
}