package com.casino.backend.controllers;

import com.casino.backend.models.User;
import com.casino.backend.models.Wallet;
import com.casino.backend.repositories.UserRepository;
import com.casino.backend.repositories.WalletRepository;
import com.casino.backend.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class AuthControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testRegister_Success() {
        User user = new User();
        user.setUsername("nuevo@gmail.com");
        user.setPassword("1234");

        when(userRepository.existsByUsername(user.getUsername())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        ResponseEntity<String> response = authController.register(user);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Usuario registrado exitosamente con bono de bienvenida de $200", response.getBody());
        verify(walletRepository, times(1)).save(any(Wallet.class));
    }

    @Test
    void testRegister_UserAlreadyExists() {
        User user = new User();
        user.setUsername("existente@gmail.com");

        when(userRepository.existsByUsername(user.getUsername())).thenReturn(true);

        ResponseEntity<String> response = authController.register(user);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Error: El usuario ya existe", response.getBody());
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    void testLogin_Success() {
        User loginRequest = new User();
        loginRequest.setUsername("test@gmail.com");
        loginRequest.setPassword("password123");

        User dbUser = new User();
        dbUser.setUsername("test@gmail.com");
        dbUser.setPassword("hashedPassword");
        dbUser.setRole("ROLE_USER");

        when(userRepository.findByUsername(loginRequest.getUsername())).thenReturn(Optional.of(dbUser));
        when(passwordEncoder.matches(loginRequest.getPassword(), dbUser.getPassword())).thenReturn(true);
        when(jwtUtil.generateToken(dbUser.getUsername(), dbUser.getRole())).thenReturn("fake-jwt-token");

        ResponseEntity<String> response = authController.login(loginRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("fake-jwt-token", response.getBody());
    }

    @Test
    void testLogin_InvalidCredentials() {
        User loginRequest = new User();
        loginRequest.setUsername("test@gmail.com");
        loginRequest.setPassword("wrongpassword");

        User dbUser = new User();
        dbUser.setUsername("test@gmail.com");
        dbUser.setPassword("hashedPassword");

        when(userRepository.findByUsername(loginRequest.getUsername())).thenReturn(Optional.of(dbUser));
        when(passwordEncoder.matches(loginRequest.getPassword(), dbUser.getPassword())).thenReturn(false);

        ResponseEntity<String> response = authController.login(loginRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Error: Credenciales inválidas", response.getBody());
    }
}