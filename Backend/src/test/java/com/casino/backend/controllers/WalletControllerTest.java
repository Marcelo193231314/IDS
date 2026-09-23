package com.casino.backend.controllers;

import com.casino.backend.models.Transaction;
import com.casino.backend.models.User;
import com.casino.backend.models.Wallet;
import com.casino.backend.repositories.TransactionRepository;
import com.casino.backend.repositories.UserRepository;
import com.casino.backend.repositories.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class WalletControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private WalletController walletController;

    private User testUser;
    private Wallet testWallet;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("marcelo@gmail.com");

        testWallet = new Wallet();
        testWallet.setUser(testUser);
        testWallet.setBalance(500.0);
    }

    @Test
    void testVipDeposit_Success() {
        Map<String, Object> request = new HashMap<>();
        request.put("cardNumber", "1234567812345678");
        request.put("amount", 200.0);

        when(userRepository.findByUsername("marcelo@gmail.com")).thenReturn(Optional.of(testUser));
        when(walletRepository.findByUserId(testUser.getId())).thenReturn(testWallet);

        ResponseEntity<?> response = walletController.vipDeposit("marcelo@gmail.com", request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(700.0, testWallet.getBalance()); 
        verify(walletRepository, times(1)).save(testWallet);
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void testVipDeposit_NegativeAmount_Blocked() {
        Map<String, Object> request = new HashMap<>();
        request.put("cardNumber", "1234567812345678");
        request.put("amount", -50.0); 

        ResponseEntity<?> response = walletController.vipDeposit("marcelo@gmail.com", request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Error: Ingresa una cantidad mayor a $0.", response.getBody());
        verify(walletRepository, never()).save(any()); 
    }

    @Test
    void testWithdraw_Success() {
        Map<String, Object> request = new HashMap<>();
        request.put("cardNumber", "1234567812345678");
        request.put("amount", 100.0);

        when(userRepository.findByUsername("marcelo@gmail.com")).thenReturn(Optional.of(testUser));
        when(walletRepository.findByUserId(testUser.getId())).thenReturn(testWallet);

        ResponseEntity<?> response = walletController.withdraw("marcelo@gmail.com", request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(400.0, testWallet.getBalance()); 
        verify(walletRepository, times(1)).save(testWallet);
    }

    @Test
    void testWithdraw_InsufficientFunds_Blocked() {
        Map<String, Object> request = new HashMap<>();
        request.put("cardNumber", "1234567812345678");
        request.put("amount", 1000.0); 

        when(userRepository.findByUsername("marcelo@gmail.com")).thenReturn(Optional.of(testUser));
        when(walletRepository.findByUserId(testUser.getId())).thenReturn(testWallet);

        ResponseEntity<?> response = walletController.withdraw("marcelo@gmail.com", request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Error: Fondos insuficientes. Tu saldo actual es menor a la cantidad solicitada.", response.getBody());
        verify(walletRepository, never()).save(any()); 
    }
}