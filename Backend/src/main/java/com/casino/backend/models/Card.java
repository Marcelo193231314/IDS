package com.casino.backend.models;

public class Card {
    private String suit;  // Corazones, Espadas, Tréboles, Diamantes
    private String rank;  // 2, 3, 4... J, Q, K, A
    private int value;    // Cuánto vale (J, Q, K = 10, A = 11)

    public Card(String suit, String rank, int value) {
        this.suit = suit;
        this.rank = rank;
        this.value = value;
    }

    // Getters y Setters
    public String getSuit() { return suit; }
    public void setSuit(String suit) { this.suit = suit; }

    public String getRank() { return rank; }
    public void setRank(String rank) { this.rank = rank; }

    public int getValue() { return value; }
    public void setValue(int value) { this.value = value; }

    // Para que al imprimirla se lea bonito (ej. "A de Corazones")
    @Override
    public String toString() {
        return rank + " de " + suit;
    }
}