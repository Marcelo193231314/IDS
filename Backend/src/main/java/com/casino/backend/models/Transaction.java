package com.casino.backend.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String type;       // Ej: "APUESTA_BLACKJACK", "PREMIO_BLACKJACK", "BONO_BIENVENIDA"
    private Double amount;     // Cuánto dinero se movió
    private Double balanceAfter; // Cómo quedó la billetera después del movimiento
    private LocalDateTime timestamp; // Fecha y hora exacta

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    public Transaction() {
        this.timestamp = LocalDateTime.now();
    }

    public Transaction(String type, Double amount, Double balanceAfter, User user) {
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.user = user;
        this.timestamp = LocalDateTime.now();
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public Double getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(Double balanceAfter) { this.balanceAfter = balanceAfter; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}