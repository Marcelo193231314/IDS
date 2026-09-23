package com.casino.backend.controllers;

import com.casino.backend.models.*;
import com.casino.backend.repositories.TransactionRepository;
import com.casino.backend.repositories.UserRepository;
import com.casino.backend.repositories.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/blackjack")
public class BlackjackController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private WalletRepository walletRepository;

    private final Map<String, BlackjackMatch> activeGames = new HashMap<>();

    private List<Card> createAndShuffleDeck() {
        List<Card> newDeck = new ArrayList<>();
        String[] suits = {"Corazones", "Espadas", "Tréboles", "Diamantes"};
        String[] ranks = {"2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A"};
        int[] values = {2, 3, 4, 5, 6, 7, 8, 9, 10, 10, 10, 10, 11};

        for (String suit : suits) {
            for (int i = 0; i < ranks.length; i++) {
                newDeck.add(new Card(suit, ranks[i], values[i]));
            }
        }
        Collections.shuffle(newDeck);
        return newDeck;
    }

    private Map<String, Object> buildGameResponse(BlackjackMatch match) {
        Map<String, Object> response = new HashMap<>();
        response.put("username", match.getUsername());
        response.put("betAmount", match.getBetAmount());
        response.put("status", match.getStatus());
        response.put("sideBetsMessage", match.getSideBetsMessage());
        response.put("playerHand", match.getPlayerHand());
        response.put("playerScore", match.calculateScore(match.getPlayerHand())); 

        if ("PLAYING".equals(match.getStatus())) {
            List<Card> visibleDealerHand = new ArrayList<>();
            visibleDealerHand.add(match.getDealerHand().get(0)); 
            response.put("dealerHand", visibleDealerHand);
            response.put("dealerScore", visibleDealerHand.get(0).getValue()); 
        } else {
            response.put("dealerHand", match.getDealerHand());
            response.put("dealerScore", match.calculateScore(match.getDealerHand()));
        }
        return response;
    }

    @PostMapping("/start")
    public Object startMatch(@RequestBody BetRequest betRequest) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();

            if (activeGames.containsKey(username)) {
                return "Error: Ya tienes una partida en curso. ¡Termínala primero!";
            }

            Optional<User> userOpt = userRepository.findByUsername(username);
            if (!userOpt.isPresent()) {
                return "Error: Tu token es viejo y tu usuario ya no existe. Haz Login nuevamente.";
            }
            User user = userOpt.get();
            Wallet wallet = walletRepository.findByUserId(user.getId());

            // BLINDAJE: Regalo de bienvenida
            if (wallet == null) {
                wallet = new Wallet();
                wallet.setUser(user);
                wallet.setBalance(1000.0);
                walletRepository.save(wallet);
            }

            double mainBet = betRequest.getAmount();
            double pairsBet = betRequest.getPerfectPairsBet();
            double totalCost = mainBet + pairsBet;

            if (wallet.getBalance() < totalCost) {
                return "Error: Saldo insuficiente. Tienes $" + wallet.getBalance();
            }

            wallet.setBalance(wallet.getBalance() - totalCost);
            if (mainBet > 0) transactionRepository.save(new Transaction("APUESTA_BLACKJACK", -mainBet, wallet.getBalance(), user));
            if (pairsBet > 0) transactionRepository.save(new Transaction("APUESTA_PARES", -pairsBet, wallet.getBalance(), user));

            BlackjackMatch match = new BlackjackMatch(username, mainBet, pairsBet, 0);
            List<Card> deck = createAndShuffleDeck();

            match.getPlayerHand().add(deck.remove(0));
            match.getDealerHand().add(deck.remove(0));
            match.getPlayerHand().add(deck.remove(0));
            match.getDealerHand().add(deck.remove(0));
            match.setDeck(deck);

            if (pairsBet > 0) {
                String card1 = match.getPlayerHand().get(0).getRank();
                String card2 = match.getPlayerHand().get(1).getRank();
                
                if (card1.equals(card2)) {
                    double pairsPrize = pairsBet * 12; 
                    wallet.setBalance(wallet.getBalance() + pairsPrize);
                    transactionRepository.save(new Transaction("PREMIO_PARES", pairsPrize, wallet.getBalance(), user));
                    match.setSideBetsMessage("¡PARES PERFECTOS! Ganaste $" + pairsPrize + ".");
                } else {
                    match.setSideBetsMessage("Perdiste tu apuesta de pares.");
                }
            }
            
            walletRepository.save(wallet); 
            activeGames.put(username, match);

            return buildGameResponse(match);
        } catch (Exception e) {
            return "ERROR REVELADO: " + e.toString();
        }
    }

    @PostMapping("/buy-insurance")
    public Object buyInsurance() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            if (!activeGames.containsKey(username)) return "Error: No tienes ninguna partida en curso.";

            BlackjackMatch match = activeGames.get(username);
            Card dealerVisibleCard = match.getDealerHand().get(0);

            if (!dealerVisibleCard.getRank().equals("A")) {
                return "Error: Solo puedes comprar seguro si el crupier muestra un As.";
            }

            User user = userRepository.findByUsername(username).get();
            Wallet wallet = walletRepository.findByUserId(user.getId());

            double insuranceCost = match.getBetAmount() / 2;
            if (wallet.getBalance() < insuranceCost) {
                return "Error: Saldo insuficiente para pagar el seguro de $" + insuranceCost;
            }

            wallet.setBalance(wallet.getBalance() - insuranceCost);
            transactionRepository.save(new Transaction("COMPRA_SEGURO", -insuranceCost, wallet.getBalance(), user));

            if (match.calculateScore(match.getDealerHand()) == 21) {
                double insurancePrize = insuranceCost * 3;
                wallet.setBalance(wallet.getBalance() + insurancePrize);
                transactionRepository.save(new Transaction("PREMIO_SEGURO", insurancePrize, wallet.getBalance(), user));
                
                match.setStatus("LOST"); 
                match.setSideBetsMessage("¡El Crupier tiene Blackjack! Cobraste tu seguro por $" + insurancePrize + ", pero pierdes la mano principal.");
                activeGames.remove(username);
            } else {
                match.setSideBetsMessage("El Crupier no tiene Blackjack. Pierdes el seguro. ¡Sigue jugando!");
            }

            walletRepository.save(wallet);
            return buildGameResponse(match);
        } catch (Exception e) {
            return "ERROR REVELADO: " + e.toString();
        }
    }

    @PostMapping("/double-down")
    public Object doubleDown() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            if (!activeGames.containsKey(username)) return "Error: No hay partida en curso.";

            BlackjackMatch match = activeGames.get(username);
            
            if (match.getPlayerHand().size() != 2) {
                return "Error: Solo puedes doblar con tu mano inicial de 2 cartas.";
            }

            User user = userRepository.findByUsername(username).get();
            Wallet wallet = walletRepository.findByUserId(user.getId());
            double currentBet = match.getBetAmount();

            if (wallet.getBalance() < currentBet) {
                return "Error: Saldo insuficiente para doblar la apuesta.";
            }

            // Descontar la apuesta extra de la billetera
            wallet.setBalance(wallet.getBalance() - currentBet);
            transactionRepository.save(new Transaction("APUESTA_DOBLE", -currentBet, wallet.getBalance(), user));
            
            // Actualizar la apuesta total en la mesa
            match.setBetAmount(currentBet * 2);

            // Repartir exactamente UNA carta extra
            match.getPlayerHand().add(match.getDeck().remove(0));
            int playerScore = match.calculateScore(match.getPlayerHand());

            // Evaluar si se pasó de 21
            if (playerScore > 21) {
                match.setStatus("LOST");
                walletRepository.save(wallet);
                activeGames.remove(username);
                return buildGameResponse(match);
            }

            // Si no se pasó, el crupier juega automáticamente (misma lógica que Stand)
            int dealerScore = match.calculateScore(match.getDealerHand());
            while (dealerScore < 17) {
                match.getDealerHand().add(match.getDeck().remove(0));
                dealerScore = match.calculateScore(match.getDealerHand());
            }

            if (dealerScore > 21 || playerScore > dealerScore) {
                match.setStatus("WON");
                double ganancia = match.getBetAmount() * 2;
                wallet.setBalance(wallet.getBalance() + ganancia);
                transactionRepository.save(new Transaction("PREMIO_DOBLE", ganancia, wallet.getBalance(), user));
            } else if (playerScore == dealerScore) {
                match.setStatus("TIE");
                wallet.setBalance(wallet.getBalance() + match.getBetAmount());
                transactionRepository.save(new Transaction("DEVOLUCION_EMPATE", match.getBetAmount(), wallet.getBalance(), user));
            } else {
                match.setStatus("LOST");
            }

            walletRepository.save(wallet);
            activeGames.remove(username);
            
            return buildGameResponse(match);
        } catch (Exception e) {
            return "ERROR REVELADO: " + e.toString();
        }
    }

    @PostMapping("/hit")
    public Object hitCard() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            if (!activeGames.containsKey(username)) return "Error: No hay partida en curso.";

            BlackjackMatch match = activeGames.get(username);
            match.getPlayerHand().add(match.getDeck().remove(0));

            if (match.calculateScore(match.getPlayerHand()) > 21) {
                match.setStatus("LOST");
                activeGames.remove(username); 
            }
            return buildGameResponse(match);
        } catch (Exception e) {
            return "ERROR REVELADO: " + e.toString();
        }
    }

    @PostMapping("/stand")
    public Object stand() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            if (!activeGames.containsKey(username)) return "Error: No hay partida en curso.";

            BlackjackMatch match = activeGames.get(username);
            int playerScore = match.calculateScore(match.getPlayerHand());
            int dealerScore = match.calculateScore(match.getDealerHand());

            while (dealerScore < 17) {
                match.getDealerHand().add(match.getDeck().remove(0));
                dealerScore = match.calculateScore(match.getDealerHand());
            }

            User user = userRepository.findByUsername(username).get();
            Wallet wallet = walletRepository.findByUserId(user.getId());

            if (dealerScore > 21 || playerScore > dealerScore) {
                match.setStatus("WON");
                double ganancia = match.getBetAmount() * 2;
                wallet.setBalance(wallet.getBalance() + ganancia);
                transactionRepository.save(new Transaction("PREMIO_BLACKJACK", ganancia, wallet.getBalance(), user));
            } else if (playerScore == dealerScore) {
                match.setStatus("TIE");
                wallet.setBalance(wallet.getBalance() + match.getBetAmount());
                transactionRepository.save(new Transaction("DEVOLUCION_EMPATE", match.getBetAmount(), wallet.getBalance(), user));
            } else {
                match.setStatus("LOST");
            }

            walletRepository.save(wallet);
            activeGames.remove(username);
            
            return buildGameResponse(match);
        } catch (Exception e) {
            return "ERROR REVELADO: " + e.toString();
        }
    }
}