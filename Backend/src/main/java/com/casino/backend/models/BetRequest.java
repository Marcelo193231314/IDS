package com.casino.backend.models;

public class BetRequest {
    private double amount;
    private double perfectPairsBet; // Apuesta para pares (Blackjack)
    private double insuranceBet;    // Apuesta para el seguro (Blackjack)
    private String guess;           // Predicción para el Coinflip (CARA o CRUZ)

    // Getters
    public double getAmount() { return amount; }
    public double getPerfectPairsBet() { return perfectPairsBet; }
    public double getInsuranceBet() { return insuranceBet; }
    public String getGuess() { return guess; }

    // Setters
    public void setAmount(double amount) { this.amount = amount; }
    public void setPerfectPairsBet(double perfectPairsBet) { this.perfectPairsBet = perfectPairsBet; }
    public void setInsuranceBet(double insuranceBet) { this.insuranceBet = insuranceBet; }
    public void setGuess(String guess) { this.guess = guess; }
}