package com.casino.backend.controllers;

import com.casino.backend.models.User;
import com.casino.backend.models.Wallet;
import com.casino.backend.repositories.UserRepository;
import com.casino.backend.repositories.WalletRepository;
import com.casino.backend.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            // Forzamos un código 400 (Bad Request)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: El usuario ya existe");
        }
        
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("ROLE_USER"); 
        
        User savedUser = userRepository.save(user);
        
        // Billetera inicializada con 200.0
        Wallet newWallet = new Wallet(200.0, savedUser);
        walletRepository.save(newWallet);
        
        // Código 200 (OK) con mensaje actualizado
        return ResponseEntity.ok("Usuario registrado exitosamente con bono de bienvenida de $200");
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody User loginRequest) {
        Optional<User> userOptional = userRepository.findByUsername(loginRequest.getUsername());
        
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                // Código 200 (OK)
                return ResponseEntity.ok(jwtUtil.generateToken(user.getUsername(), user.getRole()));
            }
        }
        // Forzamos un código 401 (Unauthorized)
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error: Credenciales inválidas");
    }
}