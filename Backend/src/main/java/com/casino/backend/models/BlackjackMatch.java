package com.casino.backend.models;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class BlackjackMatch {
    private String username;
    private double betAmount;


    public void setBetAmount(double betAmount) {
        this.betAmount = betAmount;
    }
    
    // NUEVAS APUESTAS LATERALES (Sprint 3)
    private double perfectPairsBet;
    private double insuranceBet;
    private String sideBetsMessage; // Para avisar si ganó o perdió las laterales

    private List<Card> playerHand;
    private List<Card> dealerHand;
    private String status;
    
    @JsonIgnore 
    private List<Card> deck; 

    public BlackjackMatch(String username, double betAmount, double perfectPairsBet, double insuranceBet) {
        this.username = username;
        this.betAmount = betAmount;
        this.perfectPairsBet = perfectPairsBet;
        this.insuranceBet = insuranceBet;
        this.sideBetsMessage = "Sin apuestas laterales resueltas aún.";
        this.playerHand = new ArrayList<>();
        this.dealerHand = new ArrayList<>();
        this.deck = new ArrayList<>();
        this.status = "PLAYING"; 
    }

    public int calculateScore(List<Card> hand) {
        int score = 0;
        int aces = 0;

        for (Card card : hand) {
            score += card.getValue();
            if (card.getRank().equals("A")) {
                aces++;
            }
        }
        while (score > 21 && aces > 0) {
            score -= 10;
            aces--;
        }
        return score;
    }

    // Getters y Setters Originales
    public String getUsername() { return username; }
    public double getBetAmount() { return betAmount; }
    public List<Card> getPlayerHand() { return playerHand; }
    public List<Card> getDealerHand() { return dealerHand; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<Card> getDeck() { return deck; }
    public void setDeck(List<Card> deck) { this.deck = deck; }

    // Nuevos Getters y Setters para las Apuestas Laterales
    public double getPerfectPairsBet() { return perfectPairsBet; }
    public double getInsuranceBet() { return insuranceBet; }
    public String getSideBetsMessage() { return sideBetsMessage; }
    public void setSideBetsMessage(String sideBetsMessage) { this.sideBetsMessage = sideBetsMessage; }
}